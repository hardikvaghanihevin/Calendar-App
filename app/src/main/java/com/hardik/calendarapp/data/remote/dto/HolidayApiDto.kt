package com.hardik.calendarapp.data.remote.dto


import android.os.Parcelable
import androidx.annotation.Keep
import com.hardik.calendarapp.domain.model.HolidayApiDetail
import kotlinx.android.parcel.Parcelize

@Keep
@Parcelize
data class HolidayApiDto(
    val accessRole: String,
    val defaultReminders: List<Reminder>?,  // Update to a specific type if possible
    val description: String,
    val etag: String,
    val items: List<Item>,
    val kind: String,
    val nextSyncToken: String,
    val summary: String,
    val timeZone: String,
    val updated: String
) : Parcelable {

    @Keep
    @Parcelize
    data class Item(
        val created: String,
        val creator: ItemCreator,
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
    ) : Parcelable {

        @Keep
        @Parcelize
        data class ItemCreator(
            val displayName: String,
            val email: String,
            val self: Boolean
        ) : Parcelable

        @Keep
        @Parcelize
        data class End(
            val date: String
        ) : Parcelable

        @Keep
        @Parcelize
        data class Organizer(
            val displayName: String,
            val email: String,
            val self: Boolean
        ) : Parcelable

        @Keep
        @Parcelize
        data class Start(
            val date: String
        ) : Parcelable
    }
}

// You can define a specific type for defaultReminders if possible
@Keep
@Parcelize
data class Reminder(
    val type: String, // Example, add appropriate fields
    val value: String
) : Parcelable

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
fun HolidayApiDto.Item.ItemCreator.toCreator(): HolidayApiDetail.Item.Creator {
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

