package com.mapbox.navigation.base.options

/**
 * Defines configuration for on-board router
 *
 * @param tilePath Path where tiles will be stored to / Path where tiles will be fetched from
 * @param inMemoryTileCache Max size of tiles cache (optional)
 * @param mapMatchingSpatialCache Max size of cache for map matching (optional)
 * @param threadsCount Max count of native threads (optional)
 * @param endpoint Endpoint config (optional)
 */
data class MapboxOnboardRouterConfig(
    val tilePath: String,
    val inMemoryTileCache: Int? = null,
    val mapMatchingSpatialCache: Int? = null,
    val threadsCount: Int? = null,
    val endpoint: Endpoint? = null
)
