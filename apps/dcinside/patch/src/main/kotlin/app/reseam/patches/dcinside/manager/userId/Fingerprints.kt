package app.reseam.patches.dcinside.manager.userId

import app.reseam.patch.PatchRuntime
import app.reseam.patch.Type
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.methods

context(patchRuntime: PatchRuntime)
internal fun getPostList() = methods("post list method") {
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
    returns(Type.Void)
    name("onBindViewHolder")
    literals(
        patchRuntime.resources.id("string", "post_section_text")?.toLong() ?: error("Could not find string/post_section_text"),
        patchRuntime.resources.id("string", "post_is_auto_deleted")?.toLong() ?: error("Could not find string/post_is_auto_deleted"),
    )
}.single()

context(patchRuntime: PatchRuntime)
internal fun getPostSearchList() = methods("post search list method") {
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
    returns(Type.Void)
    name("onBindViewHolder")
    literals(
        patchRuntime.resources.id("string", "post_section_text")?.toLong() ?: error("Could not find string/post_section_text"),
        patchRuntime.resources.id("attr", "textTypeSub")?.toLong() ?: error("Could not find attr/textTypeSub"),
    )
}.single()

context(patchRuntime: PatchRuntime)
internal fun getPostHeader() =  methods("post header method") {
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
    literals(
        patchRuntime.resources.id("string", "post_info_hit_counts")?.toLong() ?: error("Could not find string/post_info_hit_counts"),
        patchRuntime.resources.id("string", "auto_delete_post_date")?.toLong() ?: error("Could not find string/auto_delete_post_date"),
    )
    returns(Type.Void)
}.single()

context(patchRuntime: PatchRuntime)
internal fun getComment() = methods("reply method") {
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
    literals(
        patchRuntime.resources.id("attr", "mentionColor")?.toLong() ?: error("Could not find attr/mentionColor"),
    )
    returns(Type.Void)
}.single()

internal fun history() = methods("history method") {
    strings("postHistoryEnabled")
    calls { name("currentTimeMillis") }
}