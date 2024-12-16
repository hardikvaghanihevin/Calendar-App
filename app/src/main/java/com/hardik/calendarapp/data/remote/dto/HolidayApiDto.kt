package com.hardik.calendarapp.data.remote.dto


import androidx.annotation.Keep
import com.hardik.calendarapp.domain.model.HolidayApiDetail

@Keep
data class HolidayApiDto(
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
fun HolidayApiDto.toCalendarDetail(): HolidayApiDetail {
    return HolidayApiDetail(
        accessRole = this.accessRole,
        defaultReminders = this.defaultReminders,
        description = this.description,
        etag = this.etag,
        items = this.items.map { it.toItem() },
        kind = this.kind,
        nextSyncToken = this.nextSyncToken ?: "",
        summary = this.summary,
        timeZone = this.timeZone,
        updated = this.updated
    )
}

// Extension function to convert CalendarDto.Item to CalendarDetail.Item
fun HolidayApiDto.Item.toItem(): HolidayApiDetail.Item {
    return HolidayApiDetail.Item(
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
fun HolidayApiDto.Item.Creator.toCreator(): HolidayApiDetail.Item.Creator {
    return HolidayApiDetail.Item.Creator(
        displayName = this.displayName,
        email = this.email,
        self = this.self
    )
}

// Extension function to convert CalendarDto.End to CalendarDetail.End
fun HolidayApiDto.Item.End.toEnd(): HolidayApiDetail.Item.End {
    return HolidayApiDetail.Item.End(date = this.date)
}

// Extension function to convert CalendarDto.Organizer to CalendarDetail.Organizer
fun HolidayApiDto.Item.Organizer.toOrganizer(): HolidayApiDetail.Item.Organizer {
    return HolidayApiDetail.Item.Organizer(
        displayName = this.displayName,
        email = this.email,
        self = this.self
    )
}

// Extension function to convert CalendarDto.Start to CalendarDetail.Start
fun HolidayApiDto.Item.Start.toStart(): HolidayApiDetail.Item.Start {
    return HolidayApiDetail.Item.Start(date = this.date)
}

