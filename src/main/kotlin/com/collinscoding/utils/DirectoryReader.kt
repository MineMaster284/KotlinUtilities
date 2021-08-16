package com.collinscoding.utils

import java.io.File

/**
 * The `DirectoryReader` type defines an object that recursively reads files from a directory.
 *
 * @constructor Give a `DirectoryReader` that reads [directory].
 * @property directory The directory or file to read.
 */
class DirectoryReader(private val directory: File) {
    /**
     * Give a `DirectoryReader` that reads the `File` given by [directoryName].
     * @param directoryName The path to the directory or file to read.
     */
    constructor(directoryName: String) : this(File(directoryName))

    /**
     * The list of files, dynamically generated.
     */
    val files
        get(): List<File> = directory.listFiles()?.flatMap {
            when {
                it.isFile -> listOf(it)
                it.isDirectory -> DirectoryReader(it.absolutePath).files
                else -> listOf()
            }
        } ?: listOf(directory)
}
