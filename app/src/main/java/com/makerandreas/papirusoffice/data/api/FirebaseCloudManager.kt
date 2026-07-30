package com.makerandreas.papirusoffice.data.api

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Cloud Document model for Firestore persistence
 */
data class CloudDocument(
    val id: String = "",
    val title: String = "",
    val moduleType: String = "WRITER", // WRITER, CALC, IMPRESS, INKY
    val content: String = "",
    val authorEmail: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val sizeBytes: Long = 0L,
    val driveSyncUrl: String? = null
)

/**
 * Manages Firebase Auth & Firestore synchronization for Papirus Office documents.
 */
class FirebaseCloudManager private constructor(context: Context) {

    private var auth: FirebaseAuth? = null
    private var db: FirebaseFirestore? = null

    var isFirebaseAvailable: Boolean = false
        private set

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    enum class SyncState {
        IDLE, SYNCING, SUCCESS, ERROR
    }

    init {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            auth = FirebaseAuth.getInstance()
            db = FirebaseFirestore.getInstance()
            isFirebaseAvailable = true

            _currentUser.value = auth?.currentUser
            auth?.addAuthStateListener { firebaseAuth ->
                _currentUser.value = firebaseAuth.currentUser
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase services not initialized or google-services.json missing: ${e.localizedMessage}")
            isFirebaseAvailable = false
        }
    }

    companion object {
        private const val TAG = "FirebaseCloudManager"
        @Volatile
        private var INSTANCE: FirebaseCloudManager? = null

        fun getInstance(context: Context): FirebaseCloudManager {
            return INSTANCE ?: synchronized(this) {
                val instance = FirebaseCloudManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    /**
     * Sign in anonymously or ensure a user session exists
     */
    suspend fun ensureSignedIn(): FirebaseUser? = withContext(Dispatchers.IO) {
        val a = auth ?: return@withContext null
        val current = a.currentUser
        if (current != null) return@withContext current

        try {
            val result = a.signInAnonymously().await()
            _currentUser.value = result.user
            Log.d(TAG, "Signed in anonymously: ${result.user?.uid}")
            result.user
        } catch (e: Exception) {
            Log.e(TAG, "Anonymous sign in failed", e)
            null
        }
    }

    /**
     * Save/Sync a document to Firestore under users/{uid}/documents
     */
    suspend fun saveDocumentToCloud(document: CloudDocument): Boolean = withContext(Dispatchers.IO) {
        val database = db ?: return@withContext false
        _syncState.value = SyncState.SYNCING
        val user = ensureSignedIn()
        if (user == null) {
            _syncState.value = SyncState.ERROR
            return@withContext false
        }

        try {
            val docId = if (document.id.isEmpty()) {
                database.collection("users").document(user.uid).collection("documents").document().id
            } else {
                document.id
            }

            val docToSave = document.copy(
                id = docId,
                authorEmail = user.email ?: "anonymous@papirus.office",
                updatedAt = System.currentTimeMillis(),
                sizeBytes = document.content.toByteArray().size.toLong(),
                driveSyncUrl = "https://drive.google.com/open?id=$docId"
            )

            database.collection("users")
                .document(user.uid)
                .collection("documents")
                .document(docId)
                .set(docToSave)
                .await()

            _syncState.value = SyncState.SUCCESS
            Log.d(TAG, "Document successfully saved to Firestore: $docId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save document to Firestore", e)
            _syncState.value = SyncState.ERROR
            false
        }
    }

    /**
     * Retrieve user's cloud documents from Firestore
     */
    suspend fun fetchCloudDocuments(): List<CloudDocument> = withContext(Dispatchers.IO) {
        val database = db ?: return@withContext emptyList()
        val user = ensureSignedIn() ?: return@withContext emptyList()

        try {
            val snapshot = database.collection("users")
                .document(user.uid)
                .collection("documents")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val docs = snapshot.documents.mapNotNull { doc ->
                doc.toObject(CloudDocument::class.java)
            }
            docs
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching cloud documents", e)
            emptyList()
        }
    }

    /**
     * Delete a document from Firestore
     */
    suspend fun deleteCloudDocument(documentId: String): Boolean = withContext(Dispatchers.IO) {
        val database = db ?: return@withContext false
        val user = auth?.currentUser ?: return@withContext false
        try {
            database.collection("users")
                .document(user.uid)
                .collection("documents")
                .document(documentId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting document from Firestore", e)
            false
        }
    }

    /**
     * Sign out current user
     */
    fun signOut() {
        auth?.signOut()
        _currentUser.value = null
    }
}
