package io.budgery.api.domain.controller.imports

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.budgery.api.InstantSerializer
import domain.model.imports.Import
import java.time.Instant

class ImportDto(@JsonIgnore val import: Import) {
    val id: Int = import.id
    val fileName: String = import.filePath
    @JsonSerialize(using = InstantSerializer::class) val importedAt: Instant = import.importedAt
}