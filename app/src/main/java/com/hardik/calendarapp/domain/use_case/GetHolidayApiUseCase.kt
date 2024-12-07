package com.hardik.calendarapp.domain.use_case

import com.hardik.calendarapp.common.Resource
import com.hardik.calendarapp.data.remote.dto.toCalendarDetail
import com.hardik.calendarapp.domain.model.HolidayApiDetail
import com.hardik.calendarapp.domain.repository.HolidayApiRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

/**This method used getting holiday events from API. It's used in MainViewModel*/
class GetHolidayApiUseCase @Inject constructor(private val repository: HolidayApiRepository) {

    operator fun invoke(): Flow<Resource<HolidayApiDetail>> = flow {
        try {
            emit(Resource.Loading<HolidayApiDetail>())
            val calendar = repository.getHolidayEvents().toCalendarDetail()// data CoinDetailDto to CoinDetail transfer here
            emit(Resource.Success<HolidayApiDetail>(calendar))
        } catch(e: HttpException) {
            emit(Resource.Error<HolidayApiDetail>(e.localizedMessage ?: "An unexpected error occurred"))
        } catch(e: IOException) {
            emit(Resource.Error<HolidayApiDetail>("Couldn't reach server. Check your internet connection."))
        }
    }

}