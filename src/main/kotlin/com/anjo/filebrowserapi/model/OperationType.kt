package com.anjo.filebrowserapi.model

enum class OperationType(val description: String) {
    CREATE("create"),
    DELETE("delete"),
    RENAME("rename"),
    READ("read")
}