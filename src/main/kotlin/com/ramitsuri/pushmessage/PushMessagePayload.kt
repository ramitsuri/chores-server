package com.ramitsuri.pushmessage

data class PushMessagePayload(
    val action: PushMessageAction,
    val wontDoByOthers: List<String>,
    val doneByOthers: List<String>
) {

    fun addToDone(title: String): PushMessagePayload {
        return copy(doneByOthers = doneByOthers.plus(title))
    }

    fun addToWontDo(title: String): PushMessagePayload {
        return copy(wontDoByOthers = wontDoByOthers.plus(title))
    }

    fun toMap(): Map<String, String> {
        return mapOf(
            "action" to action.value,
            "wont_do_by_others" to wontDoByOthers.joinToString(";;;"),
            "done_by_others" to doneByOthers.joinToString(";;;"),
        )
    }

    companion object {
        fun default() = PushMessagePayload(
            action = PushMessageAction.REFRESH_TASK_ASSIGNMENTS,
            wontDoByOthers = listOf(),
            doneByOthers = listOf(),
        )
    }
}
