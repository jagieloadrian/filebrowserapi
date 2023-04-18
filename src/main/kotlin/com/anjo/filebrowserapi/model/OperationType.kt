package com.anjo.filebrowserapi.model

enum class OperationType(name: String) {
    CREATE("create"),
    DELETE("delete"),
    RENAME("rename"),
    READ("read")
}