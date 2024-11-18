package com.hardik.calendarapp.data.remote.dto


import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep
import com.hardik.calendarapp.domain.model.CalendarDetail

@Keep
data class CalendarDto(
    val accessRole: String,
    val defaultReminders: List<Any>,
    val description: String,
    val etag: String,
    val items: List<Item>,
    val kind: String,
    val nextSyncToken: String,
    val summary: String,
    val timeZone: String,
    val updated: String
) {
    @Keep
    data class Item(
        val created: String,
        val creator: Creator,
        val description: String,
        val end: End,
        val etag: String,
        val eventType: String,
        val htmlLink: String,
        val iCalUID: String,
        val id: String,
        val kind: String,
        val organizer: Organizer,
        val sequence: Int,
        val start: Start,
        val status: String,
        val summary: String,
        val transparency: String,
        val updated: String,
        val visibility: String
    ) {
        @Keep
        data class Creator(
            val displayName: String,
            val email: String,
            val self: Boolean
        )

        @Keep
        data class End(
            val date: String
        )

        @Keep
        data class Organizer(
            val displayName: String,
            val email: String,
            val self: Boolean
        )

        @Keep
        data class Start(
            val date: String
        )
    }
}


// Extension function to convert CalendarDto to CalendarDetail
fun CalendarDto.toCalendarDetail(): CalendarDetail {
    return CalendarDetail(
        accessRole = this.accessRole,
        defaultReminders = this.defaultReminders,
        description = this.description,
        etag = this.etag,
        items = this.items.map { it.toItem() },
        kind = this.kind,
        nextSyncToken = this.nextSyncToken,
        summary = this.summary,
        timeZone = this.timeZone,
        updated = this.updated
    )
}

// Extension function to convert CalendarDto.Item to CalendarDetail.Item
fun CalendarDto.Item.toItem(): CalendarDetail.Item {
    return CalendarDetail.Item(
        created = this.created,
        creator = this.creator.toCreator(),
        description = this.description,
        end = this.end.toEnd(),
        etag = this.etag,
        eventType = this.eventType,
        htmlLink = this.htmlLink,
        iCalUID = this.iCalUID,
        id = this.id,
        kind = this.kind,
        organizer = this.organizer.toOrganizer(),
        sequence = this.sequence,
        start = this.start.toStart(),
        status = this.status,
        summary = this.summary,
        transparency = this.transparency,
        updated = this.updated,
        visibility = this.visibility
    )
}

// Extension function to convert CalendarDto.Creator to CalendarDetail.Creator
fun CalendarDto.Item.Creator.toCreator(): CalendarDetail.Item.Creator {
    return CalendarDetail.Item.Creator(
        displayName = this.displayName,
        email = this.email,
        self = this.self
    )
}

// Extension function to convert CalendarDto.End to CalendarDetail.End
fun CalendarDto.Item.End.toEnd(): CalendarDetail.Item.End {
    return CalendarDetail.Item.End(date = this.date)
}

// Extension function to convert CalendarDto.Organizer to CalendarDetail.Organizer
fun CalendarDto.Item.Organizer.toOrganizer(): CalendarDetail.Item.Organizer {
    return CalendarDetail.Item.Organizer(
        displayName = this.displayName,
        email = this.email,
        self = this.self
    )
}

// Extension function to convert CalendarDto.Start to CalendarDetail.Start
fun CalendarDto.Item.Start.toStart(): CalendarDetail.Item.Start {
    return CalendarDetail.Item.Start(date = this.date)
}

