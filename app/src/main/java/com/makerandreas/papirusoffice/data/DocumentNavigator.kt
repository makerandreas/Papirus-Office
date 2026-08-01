package com.makerandreas.papirusoffice.data

interface DocumentNavigator {
    fun goToPage(page: Int)
    fun currentPage(): Int
    fun pageCount(): Int
    fun previousPage() {
        val curr = currentPage()
        if (curr > 1) goToPage(curr - 1)
    }
    fun nextPage() {
        val curr = currentPage()
        if (curr < pageCount()) goToPage(curr + 1)
    }
}
