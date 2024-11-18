package com.hardik.calendarapp.domain.use_case

import com.hardik.calendarapp.common.Resource
import com.hardik.calendarapp.data.remote.dto.toCalendarDetail
import com.hardik.calendarapp.domain.model.CalendarDetail
import com.hardik.calendarapp.domain.repository.CalendarRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

/**This method used getting holiday events from API. It's used in MainViewModel*/
class GetCalendarUseCase @Inject constructor(private val repository: CalendarRepository) {

    operator fun invoke(): Flow<Resource<CalendarDetail>> = flow {
        try {
            emit(Resource.Loading<CalendarDetail>())
            val calendar = repository.getCalendar().toCalendarDetail()// data CoinDetailDto to CoinDetail transfer here
            emit(Resource.Success<CalendarDetail>(calendar))
        } catch(e: HttpException) {
            emit(Resource.Error<CalendarDetail>(e.localizedMessage ?: "An unexpected error occurred"))
        } catch(e: IOException) {
            emit(Resource.Error<CalendarDetail>("Couldn't reach server. Check your internet connection."))
        }
    }

}