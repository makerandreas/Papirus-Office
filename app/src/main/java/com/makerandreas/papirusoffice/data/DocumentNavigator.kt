package com.makerandreas.papirusoffice.data

interface DocumentNavigator {
    fun goToPage(page: Int)
    fun currentPage(): Int
    fun pageCount(): Int
}
