package com.mkchtv.cleantemplate.domain.element.usecase

import com.mkchtv.cleantemplate.domain.element.repository.ElementRepository
import javax.inject.Inject

class DeleteElement @Inject constructor(
    private val repository: ElementRepository,
) {

    suspend operator fun invoke(vararg ids: Int) = repository.delete(ids = ids)
}
