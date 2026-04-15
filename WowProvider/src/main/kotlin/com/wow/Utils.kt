package com.wow

fun String.regexFindAll(regex: String): List<String> {
    return Regex(regex).findAll(this).map { it.value }.toList()
}
