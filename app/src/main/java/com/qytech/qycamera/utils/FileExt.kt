package com.qytech.qycamera.utils

import timber.log.Timber
import java.io.File

/**
 *  Environment.DIRECTORY_MUSIC
 *  Environment.DIRECTORY_PODCASTS
 *  Environment.DIRECTORY_RINGTONES
 *  Environment.DIRECTORY_ALARMS
 *  Environment.DIRECTORY_NOTIFICATIONS
 *  Environment.DIRECTORY_PICTURES
 *  Environment.DIRECTORY_MOVIES.
 * */
val EXTENSIONS_PICTURES =
    listOf(".PNG", ".JPG", ".JPEG", ".BMP", ".GIF", ".WEBP", ".DNG", ".TIF", ".EPS", ".RAW")
val EXTENSIONS_MOVIES = listOf(".AVI", ".MP4", ".FLV", ".ASF", ".MKV", ".MOV")
val EXTENSIONS_DOCUMENT = listOf(
    ".DOC",
    ".DOCX",
    ".HTML",
    ".ODT",
    ".PDF",
    ".XLS",
    ".XLSX",
    ".ODS",
    ".PPT",
    ".PPTX",
    ".TXT"
)
val EXTENSIONS_MUSIC = listOf(".M4A", ".FLAC", ".MP3", ".WAV", ".AAC")

val File.extension: String
    get() = ".${name.substringAfterLast('.', "")}"

val File.isMusic: Boolean
    get() = extension.uppercase() in EXTENSIONS_MUSIC

val File.isPictures: Boolean
    get() = extension.uppercase() in EXTENSIONS_PICTURES

val File.isMovies: Boolean
    get() = extension.uppercase() in EXTENSIONS_MOVIES

val File.isDocuments: Boolean
    get() = extension.uppercase() in EXTENSIONS_DOCUMENT
