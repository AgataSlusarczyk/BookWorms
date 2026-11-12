package com.bookworms.server.copies

import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.io.Serializable
import java.util.UUID

// "My copy can be seen in this group" – the owner picks the groups on My copy.
@Entity
@Table(name = "copy_group_visibility")
class CopyGroupVisibility(
	@EmbeddedId
	val id: CopyGroupVisibilityId,
)

@Embeddable
data class CopyGroupVisibilityId(
	val copyId: UUID,
	val groupId: UUID,
) : Serializable

interface CopyGroupVisibilityRepository : JpaRepository<CopyGroupVisibility, CopyGroupVisibilityId> {

	// "Id_CopyId" means id.copyId – the copy part of the key.

	// Where these copies can be seen – one query for the whole My books list.
	fun findById_CopyIdIn(copyIds: Collection<UUID>): List<CopyGroupVisibility>

	fun deleteById_CopyId(copyId: UUID)

	// The copies a group can see.
	fun findById_GroupId(groupId: UUID): List<CopyGroupVisibility>
}
