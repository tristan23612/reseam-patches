package app.reseam.patches.dcinside.homeElements

import app.reseam.patch.methods

internal fun bestPickItem(layoutResourceId: Long) = methods("bestPickItem method") {
    literals(layoutResourceId)
}.single()