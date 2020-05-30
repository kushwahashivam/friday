package com.ai.friday.config

class Config {
    companion object {
        const val TOKEN_FILE_NAME = "token.txt"
        const val TASKS_FILE_NAME = "tasks.json"
        const val SYSTEM_INFO_UPDATE_DURATION = 5000L
        const val TASK_PROGRESS_UPDATE_DURATION = 5000L

        const val CODE_SUCCESS = 10
        const val CODE_INSUFFICIENT_DATA = 12
        val CODES_AUTH_ERROR = listOf(13, 14, 15, 16, 17)
        const val CODE_INVALID_CREDENTIALS = 18
        const val CODE_TASK_NOT_FOUND = 24
        const val CODE_TASK_NOT_COMPLETED = 25
        const val MAX_IMAGE_SIZE = 1920

        const val URL_HOME = "http://192.168.1.101:8080/"
        const val URL_LOGIN = "${URL_HOME}login"
        const val URL_GET_FACE_IMAGE = "${URL_HOME}get-face-image"
        const val URL_GET_PERSON_DATA = "${URL_HOME}get-person-data"
        const val URL_SEARCH_PERSON = "${URL_HOME}search-person"
        const val URL_DETECT_FACES = "${URL_HOME}detect-faces"
        const val URL_RECOGNIZE_FACES = "${URL_HOME}search-matching-faces"
        const val URL_GET_TASK_PROGRESS = "${URL_HOME}get-task-progress"
        const val URL_GET_TASK_RESULT = "${URL_HOME}get-task-result"
        const val URL_ADD_STAR_TO_PERSON = "${URL_HOME}add-star-to-person"
        const val URL_REMOVE_STAR_FROM_PERSON = "${URL_HOME}remove-star-from-person"
        const val URL_GET_STARRED_PERSONS = "${URL_HOME}get-starred-persons"

        const val TASK_INVESTIGATE_IMAGE = 1
    }
}