package me.anon.grow3.data.repository.impl

import android.content.res.Resources
import androidx.lifecycle.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.anon.grow3.data.model.Diary
import me.anon.grow3.data.repository.DiariesRepository
import me.anon.grow3.data.source.DiariesDataSource
import me.anon.grow3.util.states.DataResult
import me.anon.grow3.util.states.asSuccess
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class DefaultDiariesRepository @Inject constructor(
	private val dataSource: DiariesDataSource,
	@Named("io_dispatcher") private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : DiariesRepository
{
	private val _refresh = MutableLiveData(true)
	private val _diaries = _refresh.switchMap { force ->
		liveData<DataResult<List<Diary>>> {
			emit(DataResult.Success(dataSource.sync(DiariesDataSource.SyncDirection.LOAD)))
		}
	}

	override fun observeDiaries(): LiveData<DataResult<List<Diary>>> = _diaries

	override fun observeDiary(diaryId: String): LiveData<DataResult<Diary>> = _diaries.map {
		if (it is DataResult.Success)
		{
			it.data.firstOrNull { it.id == diaryId }
				?.let { DataResult.success(it) }
				?: DataResult.Error(Resources.NotFoundException())
		}
		else DataResult.Error(Resources.NotFoundException())
	}

	override suspend fun getDiaries(): List<Diary> = dataSource.getDiaries()

	override suspend fun getDiaryById(diaryId: String): Diary? = dataSource.getDiaryById(diaryId)

	override suspend fun createDiary(diary: Diary): Diary
	{
		return dataSource.addDiary(diary)
			.find { it.id == diary.id }!!
			.also {
				invalidate()
			}
	}

	override suspend fun deleteDiary(diaryId: String): Boolean
	{
		return !dataSource.deleteDiary(diaryId)
			.any { it.id == diaryId }
			.also {
				invalidate()
			}
	}

	override fun sync()
	{
		_diaries.value?.asSuccess()?.let { diaries ->
			CoroutineScope(dispatcher).launch {
				dataSource.sync(DiariesDataSource.SyncDirection.SAVE, *diaries.toTypedArray())
				invalidate()
			}
		}
	}

	override fun invalidate()
	{
		_refresh.postValue(true)
	}
}