package com.stormunblessed

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.M3u8Helper.Companion.generateM3u8
import com.lagradost.cloudstream3.utils.getQualityFromName

class KronchENProvider: MainAPI() {
    companion object {
        var latestHeader: Map<String, String> = emptyMap()
        var latestKrunchyHeader: Map<String, String> = emptyMap()
        var latestKrunchySession: Map<String, String> = emptyMap()
        var latestcountryID = ""
        private const val krunchyapi = "https://beta-api.crunchyroll.com"
        private const val kronchyConsumetapi = "https://api.consumet.org/anime/crunchyroll"
    }
    override var name = "Kronch"
    override var mainUrl = "https://consumet.org"
    override val instantLinkLoading = false
    override val hasMainPage = true
    override var lang = "en"
    override val supportedTypes = setOf(
        TvType.AnimeMovie,
        TvType.Anime,
    )

    data class KamyToken (
        @JsonProperty("access_token" ) val accessToken : String,
        @JsonProperty("token_type"   ) val tokenType   : String,
        @JsonProperty("expires_in"   ) val expiresIn   : Int?=null
    )

    data class KrunchyToken (
        @JsonProperty("access_token" ) val accessToken : String? = null,
        @JsonProperty("expires_in"   ) val expiresIn   : Int?    = null,
        @JsonProperty("token_type"   ) val tokenType   : String? = null,
        @JsonProperty("scope"        ) val scope       : String? = null,
        @JsonProperty("country"      ) val country     : String? = null
    )

    private suspend fun getKrunchyToken(): Map<String, String> {
        val testingasa = app.post("$krunchyapi/auth/v1/token",
            headers = mapOf(
                "User-Agent"  to "Crunchyroll/3.26.1 Android/11 okhttp/4.9.2",
                "Content-Type" to "application/x-www-form-urlencoded",
                "Authorization" to "Basic aHJobzlxM2F3dnNrMjJ1LXRzNWE6cHROOURteXRBU2Z6QjZvbXVsSzh6cUxzYTczVE1TY1k="
            ),
            data = mapOf("grant_type" to "client_id")
        ).parsed<KrunchyToken>()
        val header = mapOf(
            "Authorization" to "${testingasa.tokenType} ${testingasa.accessToken}"
        )
        val countryID = testingasa.country!!
        latestKrunchyHeader = header
        latestcountryID = countryID
        return latestKrunchyHeader
    }

    data class PosterTall (
        @JsonProperty("height" ) var height : Int?    = null,
        @JsonProperty("source" ) var source : String? = null,
        @JsonProperty("type"   ) var type   : String? = null,
        @JsonProperty("width"  ) var width  : Int?    = null
    )


    data class KrunchyHome (
        @JsonProperty("total"            ) val total         : Int?             = null,
        @JsonProperty("items"            ) val items         : ArrayList<KrunchyItems> = arrayListOf(),
        @JsonProperty("__class__"        ) val _class_       : String?          = null,
        @JsonProperty("__href__"         ) val _href_        : String?          = null,
        @JsonProperty("__resource_key__" ) val _resourceKey_ : String?          = null,
    )

    data class KrunchyItems (
        @JsonProperty("__class__"           ) var _class_           : String?         = null,
        @JsonProperty("new_content"         ) var newContent        : Boolean?        = null,
        @JsonProperty("description"         ) var description       : String?         = null,
        @JsonProperty("__href__"            ) var _href_            : String?         = null,
        @JsonProperty("title"               ) var title             : String?         = null,
        @JsonProperty("promo_description"   ) var promoDescription  : String?         = null,
        @JsonProperty("slug"                ) var slug              : String?         = null,
        @JsonProperty("channel_id"          ) var channelId         : String?         = null,
        @JsonProperty("images"              ) var images            : KrunchyImages?         = KrunchyImages(),
        @JsonProperty("linked_resource_key" ) var linkedResourceKey : String?         = null,
        @JsonProperty("last_public"         ) var lastPublic        : String?         = null,
        @JsonProperty("slug_title"          ) var slugTitle         : String?         = null,
        @JsonProperty("external_id"         ) var externalId        : String?         = null,
        @JsonProperty("series_metadata"     ) var seriesMetadata    : KrunchySeriesMetadata? = KrunchySeriesMetadata(),
        @JsonProperty("type"                ) var type              : String?         = null,
        @JsonProperty("id"                  ) var id                : String?         = null,
        @JsonProperty("promo_title"         ) var promoTitle        : String?         = null,
        @JsonProperty("new"                 ) var new               : Boolean?        = null
    )

    data class KrunchyImages (
        @JsonProperty("poster_tall" ) var posterTall : ArrayList<ArrayList<PosterTall>> = arrayListOf(),
        @JsonProperty("poster_wide" ) var posterWide : ArrayList<ArrayList<PosterTall>> = arrayListOf(),
        @JsonProperty("thumbnail" ) var thumbnail : ArrayList<ArrayList<PosterTall>> = arrayListOf(),
    )
    data class KrunchySeriesMetadata (
        @JsonProperty("audio_locales"            ) var audioLocales           : ArrayList<String>       = arrayListOf(),
        @JsonProperty("availability_notes"       ) var availabilityNotes      : String?                 = null,
        @JsonProperty("episode_count"            ) var episodeCount           : Int?                    = null,
        @JsonProperty("extended_description"     ) var extendedDescription    : String?                 = null,
        @JsonProperty("is_dubbed"                ) var isDubbed               : Boolean?                = null,
        @JsonProperty("is_mature"                ) var isMature               : Boolean?                = null,
        @JsonProperty("is_simulcast"             ) var isSimulcast            : Boolean?                = null,
        @JsonProperty("is_subbed"                ) var isSubbed               : Boolean?                = null,
        @JsonProperty("mature_blocked"           ) var matureBlocked          : Boolean?                = null,
        @JsonProperty("maturity_ratings"         ) var maturityRatings        : ArrayList<String>       = arrayListOf(),
        @JsonProperty("season_count"             ) var seasonCount            : Int?                    = null,
        @JsonProperty("series_launch_year"       ) var seriesLaunchYear       : Int?                    = null,
        @JsonProperty("subtitle_locales"         ) var subtitleLocales        : ArrayList<String>       = arrayListOf()
    )





    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val items = ArrayList<HomePageList>()
        val urls = listOf(
            Pair("$krunchyapi/content/v1/browse?locale=en-US&n=20&sort_by=popularity", "Trending"),
            Pair("$krunchyapi/content/v1/browse?locale=en-US&n=20&sort_by=newly_added", "Newly Added")
        )
        getKrunchyToken()
        urls.apmap {(url, name) ->
            val res = app.get(url,
                headers = latestKrunchyHeader
            ).parsed<KrunchyHome>()
            val home = res.items.map {
                val title = it.title
                val posterstring = it.images?.posterTall.toString()
                val posterregex = Regex("height=2340.*source=(.*),.*type=poster_tall")
                val poster = posterregex.find(posterstring)?.destructured?.component1() ?: ""
                val seriesID = it.id
                val data = "{\"tvtype\":\"${it.type}\",\"seriesID\":\"$seriesID\"}"
                newAnimeSearchResponse(title!!, data){
                    this.posterUrl = poster
                }
            }
            items.add(HomePageList(name, home))
        }
        if (items.size <= 0) throw ErrorLoadingException()
        return HomePageResponse(items)
    }

    data class KrunchySearch (

        @JsonProperty("total"            ) var total         : Int?             = null,
        @JsonProperty("items"            ) var items         : ArrayList<KrunchyItemsOne> = arrayListOf(),
    )


    data class KrunchyItemsOne (
        @JsonProperty("type"             ) var type          : String?          = null,
        @JsonProperty("total"            ) var total         : Int?             = null,
        @JsonProperty("items"            ) var items         : ArrayList<KrunchySearchItems> = arrayListOf(),
        @JsonProperty("__class__"        ) var _class_       : String?          = null,
        @JsonProperty("__href__"         ) var _href_        : String?          = null,
        @JsonProperty("__resource_key__" ) var _resourceKey_ : String?          = null,
    )
    data class KrunchySearchItems (
        @JsonProperty("id"                  ) var id                : String?         = null,
        @JsonProperty("new"                 ) var new               : Boolean?        = null,
        @JsonProperty("new_content"         ) var newContent        : Boolean?        = null,
        @JsonProperty("type"                ) var type              : String?         = null,
        @JsonProperty("channel_id"          ) var channelId         : String?         = null,
        @JsonProperty("__href__"            ) var _href_            : String?         = null,
        @JsonProperty("title"               ) var title             : String?         = null,
        @JsonProperty("slug"                ) var slug              : String?         = null,
        @JsonProperty("description"         ) var description       : String?         = null,
        @JsonProperty("series_metadata"     ) var seriesMetadata    : KrunchySearchSeriesMetadata? = KrunchySearchSeriesMetadata(),
        @JsonProperty("__class__"           ) var _class_           : String?         = null,
        @JsonProperty("external_id"         ) var externalId        : String?         = null,
        @JsonProperty("images"              ) var images            : KrunchyImages?         = KrunchyImages(),
        @JsonProperty("promo_description"   ) var promoDescription  : String?         = null,
        @JsonProperty("promo_title"         ) var promoTitle        : String?         = null,
        @JsonProperty("slug_title"          ) var slugTitle         : String?         = null,
    )
    data class KrunchySearchSeriesMetadata (
        @JsonProperty("audio_locales"            ) var audioLocales           : ArrayList<String>       = arrayListOf(),
        @JsonProperty("availability_notes"       ) var availabilityNotes      : String?                 = null,
        @JsonProperty("episode_count"            ) var episodeCount           : Int?                    = null,
        @JsonProperty("extended_description"     ) var extendedDescription    : String?                 = null,
        @JsonProperty("is_dubbed"                ) var isDubbed               : Boolean?                = null,
        @JsonProperty("is_mature"                ) var isMature               : Boolean?                = null,
        @JsonProperty("is_simulcast"             ) var isSimulcast            : Boolean?                = null,
        @JsonProperty("is_subbed"                ) var isSubbed               : Boolean?                = null,
        @JsonProperty("mature_blocked"           ) var matureBlocked          : Boolean?                = null,
        @JsonProperty("maturity_ratings"         ) var maturityRatings        : ArrayList<String>       = arrayListOf(),
        @JsonProperty("season_count"             ) var seasonCount            : Int?                    = null,
        @JsonProperty("series_launch_year"       ) var seriesLaunchYear       : Int?                    = null,
        @JsonProperty("subtitle_locales"         ) var subtitleLocales        : ArrayList<String>       = arrayListOf()

    )

    override suspend fun search(query: String): List<SearchResponse> {
        getKrunchyToken()
        val main = app.get("$krunchyapi/content/v1/search",
            headers = latestKrunchyHeader,
            params = mapOf(
                "q" to query,
                "locale" to "en-US"
            )
        ).parsedSafe<KrunchySearch>()
        val search = ArrayList<SearchResponse>()

        main?.items?.map {
            if (it.type?.contains(Regex("movie|series")) == true) {
                val type1 = it.type
                it.items.map {
                    val title = it.title
                    val id = it.id
                    val data = "{\"tvtype\":\"$type1\",\"seriesID\":\"$id\"}"
                    val posterstring = it.images?.posterTall.toString()
                    val posterregex = Regex("height=2340.*source=(.*),.*type=poster_tall")
                    val poster = posterregex.find(posterstring)?.destructured?.component1() ?: ""
                    search.add(newAnimeSearchResponse(title!!, data){
                        this.posterUrl = poster
                    })
                }
            }
        }

        return search
    }


    data class KamySeasons (
        @JsonProperty("items"     ) var items   : ArrayList<ItemsSeason> = arrayListOf()
    )
    data class ItemsSeason (
        @JsonProperty("id"            ) var id           : String?             = null,
        @JsonProperty("channel_id"    ) var channelId    : String?             = null,
        @JsonProperty("title"         ) var title        : String?             = null,
        @JsonProperty("slug_title"    ) var slugTitle    : String?             = null,
        @JsonProperty("series_id"     ) var seriesId     : String?             = null,
        @JsonProperty("season_number" ) var seasonNumber : Int?                = null,
        @JsonProperty("description"   ) var description  : String?             = null,
        @JsonProperty("episodes"      ) var episodes     : ArrayList<Episodes> = arrayListOf(),
        @JsonProperty("episode_count" ) var episodeCount : Int?                = null
    )

    data class Episodes (
        @JsonProperty("id"                ) var id              : String?  = null,
        @JsonProperty("channel_id"        ) var channelId       : String?  = null,
        @JsonProperty("series_id"         ) var seriesId        : String?  = null,
        @JsonProperty("series_title"      ) var seriesTitle     : String?  = null,
        @JsonProperty("series_slug_title" ) var seriesSlugTitle : String?  = null,
        @JsonProperty("season_id"         ) var seasonId        : String?  = null,
        @JsonProperty("season_title"      ) var seasonTitle     : String?  = null,
        @JsonProperty("season_slug_title" ) var seasonSlugTitle : String?  = null,
        @JsonProperty("season_number"     ) var seasonNumber    : Int?     = null,
        @JsonProperty("episode"           ) var episode         : String?  = null,
        @JsonProperty("episode_number"    ) var episodeNumber   : Int?     = null,
        @JsonProperty("sequence_number"   ) var sequenceNumber  : Int?     = null,
        @JsonProperty("title"             ) var title           : String?  = null,
        @JsonProperty("slug_title"        ) var slugTitle       : String?  = null,
        @JsonProperty("description"       ) var description     : String?  = null,
        @JsonProperty("hd_flag"           ) var hdFlag          : Boolean? = null,
        @JsonProperty("is_mature"         ) var isMature        : Boolean? = null,
        @JsonProperty("episode_air_date"  ) var episodeAirDate  : String?  = null,
        @JsonProperty("is_subbed"         ) var isSubbed        : Boolean? = null,
        @JsonProperty("is_dubbed"         ) var isDubbed        : Boolean? = null,
        @JsonProperty("is_clip"           ) var isClip          : Boolean? = null,
        @JsonProperty("type"              ) var type            : String?  = null,
        @JsonProperty("images"            ) var images          : ImagesEps?  = ImagesEps(),
        @JsonProperty("duration_ms"       ) var durationMs      : Int?     = null,
        @JsonProperty("is_premium_only"   ) var isPremiumOnly   : Boolean? = null
    )

    data class ImagesEps (
        @JsonProperty("thumbnail" ) var thumbnail : ArrayList<Thumbnail> = arrayListOf()
    )
    data class Thumbnail (
        @JsonProperty("width"  ) var width  : Int?    = null,
        @JsonProperty("height" ) var height : Int?    = null,
        @JsonProperty("type"   ) var type   : String? = null,
        @JsonProperty("source" ) var source : String? = null
    )


    data class KrunchyLoadMain (
        @JsonProperty("total" ) var total : Int?            = null,
        @JsonProperty("data"  ) var data  : ArrayList<KrunchyMetadata> = arrayListOf(),
    )

    data class KrunchyMetadata (
        @JsonProperty("series_launch_year"       ) var seriesLaunchYear       : Int?                    = null,
        @JsonProperty("title"                    ) var title                  : String?                 = null,
        @JsonProperty("keywords"                 ) var keywords               : ArrayList<String>       = arrayListOf(),
        @JsonProperty("content_provider"         ) var contentProvider        : String?                 = null,
        @JsonProperty("subtitle_locales"         ) var subtitleLocales        : ArrayList<String>       = arrayListOf(),
        @JsonProperty("is_dubbed"                ) var isDubbed               : Boolean?                = null,
        @JsonProperty("audio_locales"            ) var audioLocales           : ArrayList<String>       = arrayListOf(),
        @JsonProperty("season_tags"              ) var seasonTags             : ArrayList<String>       = arrayListOf(),
        @JsonProperty("episode_count"            ) var episodeCount           : Int?                    = null,
        @JsonProperty("season_count"             ) var seasonCount            : Int?                    = null,
        @JsonProperty("is_subbed"                ) var isSubbed               : Boolean?                = null,
        @JsonProperty("channel_id"               ) var channelId              : String?                 = null,
        @JsonProperty("extended_description"     ) var extendedDescription    : String?                 = null,
        @JsonProperty("seo_description"          ) var seoDescription         : String?                 = null,
        @JsonProperty("is_simulcast"             ) var isSimulcast            : Boolean?                = null,
        @JsonProperty("availability_notes"       ) var availabilityNotes      : String?                 = null,
        @JsonProperty("slug"                     ) var slug                   : String?                 = null,
        @JsonProperty("maturity_ratings"         ) var maturityRatings        : ArrayList<String>       = arrayListOf(),
        @JsonProperty("mature_blocked"           ) var matureBlocked          : Boolean?                = null,
        @JsonProperty("images"                   ) var images                 : KrunchyImages?                 = KrunchyImages(),
        @JsonProperty("media_count"              ) var mediaCount             : Int?                    = null,
        @JsonProperty("id"                       ) var id                     : String?                 = null,
        @JsonProperty("slug_title"               ) var slugTitle              : String?                 = null,
        @JsonProperty("description"              ) var description            : String?                 = null,
        @JsonProperty("is_mature"                ) var isMature               : Boolean?                = null,
        @JsonProperty("seo_title"                ) var seoTitle               : String?                 = null,
        @JsonProperty("premium_available_date"    ) var premiumAvailableDate    : String?                 = null,
        @JsonProperty("closed_captions_available" ) var closedCaptionsAvailable : Boolean?                = null,
        @JsonProperty("free_available_date"       ) var freeAvailableDate       : String?                 = null,
        @JsonProperty("available_date"            ) var availableDate           : String?                 = null,
        @JsonProperty("media_type"                ) var mediaType               : String?                 = null,
        @JsonProperty("available_offline"         ) var availableOffline        : Boolean?                = null,
        @JsonProperty("premium_date"              ) var premiumDate             : String?                 = null,
        @JsonProperty("movie_listing_title"       ) var movieListingTitle       : String?                 = null,
        @JsonProperty("duration_ms"               ) var durationMs              : Int?                    = null,
        @JsonProperty("is_premium_only"           ) var isPremiumOnly           : Boolean?                = null,
        @JsonProperty("listing_id"                ) var listingId               : String?                 = null,
    )


    private suspend fun getKrunchySeasonsInfo(fixedID: String?): KrunchySeasonsInfo {
        getKrunchyToken()
        val url = "$krunchyapi/content/v2/cms/series/$fixedID/seasons?locale=en-US"
        val responsText = app.get(url, headers = latestKrunchyHeader).text
        return parseJson(responsText)
    }

    private suspend fun getKrunchyDubEpisodes(fixedID: String?): ArrayList<Episode> {
        val krunchyResponse = getKrunchySeasonsInfo(fixedID)
        val dubEp = ArrayList<Episode>()
        getKrunchyToken()
        krunchyResponse.data.map {
            val versions = it.versions
            if (versions != null) {
                it.versions!!.map {
                    val audioLocale = it.audioLocale
                    val guid = it.guid
                    val versionsResponse = app.get("$krunchyapi/content/v2/cms/seasons/$guid/episodes?locale=en-US", headers = latestKrunchyHeader).parsed<KrunchySeasonsInfo>()
                    if (audioLocale == "en-US") {
                        versionsResponse.data.map {
                            val title = it.title
                            val epID = it.id
                            val epdesc = it.description
                            val posterstring = it.images?.thumbnail.toString()
                            val posterRegex = Regex("PosterTall\\(height=338,.*source=(.*),.*type=thumbnail,.width=600\\)")
                            val poster = posterRegex.find(posterstring)?.destructured?.component1()
                            dubEp.add(
                                Episode(
                                    name = title,
                                    data = epID!!,
                                    description = epdesc,
                                    posterUrl = poster
                                )
                            )
                        }
                    }
                }
            }
        }
        return dubEp
    }


    private suspend fun getKrunchySubEpisodes(fixedID: String?): ArrayList<Episode> {
        val subEp = ArrayList<Episode>()
        getKrunchyToken()
        val krunchyresponse = getKrunchySeasonsInfo(fixedID)
        krunchyresponse.data.apmap {
            val versions = it.versions
            val audiolocaleSUPER = it.audioLocale
            if (!versions.isNullOrEmpty()) {
                it.versions!!.map {
                    val audioLocale = it.audioLocale
                    val guid = it.guid
                    val versionsResponse = app.get("$krunchyapi/content/v2/cms/seasons/$guid/episodes?locale=en-US", headers = latestKrunchyHeader).parsed<KrunchySeasonsInfo>()
                    if (audioLocale!!.contains(Regex("ja-JP|zh-CN"))) {
                        versionsResponse.data.map {
                            val title = it.title
                            val epID = it.id
                            val epdesc = it.description
                            val posterstring = it.images?.thumbnail.toString()
                            val posterRegex = Regex("PosterTall\\(height=338,.*source=(.*),.*type=thumbnail,.width=600\\)")
                            val poster = posterRegex.find(posterstring)?.destructured?.component1()
                            subEp.add(
                                Episode(
                                    name = title,
                                    data = epID!!,
                                    description = epdesc,
                                    posterUrl = poster
                                )
                            )
                        }
                    }
                }
            }

            if (!audiolocaleSUPER.isNullOrEmpty() && audiolocaleSUPER.contains(Regex("ja-JP|zh-CN")) && versions == null) {
                val seriesID = it.id
                val seasontitle = it.title
                val responseSUBS = app.get("$krunchyapi/content/v2/cms/seasons/$seriesID/episodes?locale=en-US", headers = latestKrunchyHeader).parsed<KrunchySeasonsInfo>()
                responseSUBS.data.map {
                    val title = it.title
                    val epID = it.id
                    val epdesc = it.description
                    val posterstring = it.images?.thumbnail.toString()
                    val posterRegex = Regex("PosterTall\\(height=338,.*source=(.*),.*type=thumbnail,.width=600\\)")
                    val poster = posterRegex.find(posterstring)?.destructured?.component1()
                    val ep = Episode(
                        name = title,
                        data = epID!!,
                        description = epdesc,
                        posterUrl = poster
                    )
                    if (seasontitle!!.contains(Regex("Piece: East Blue|Piece: Alabasta|Piece: Sky Island"))) {
                        //nothing, to filter out non HD EPS
                    } else if (!seasontitle.contains(Regex("Dub"))) {
                        subEp.add(ep)
                    }
                }
            }
        }
        return subEp
    }

    data class KrunchySeasonsInfo (
        @JsonProperty("total" ) var total : Int?            = null,
        @JsonProperty("data"  ) var data  : ArrayList<KrunchySeasonsData> = arrayListOf(),
    )

    data class KrunchySeasonsData (
        @JsonProperty("maturity_ratings"          ) var maturityRatings         : ArrayList<String>       = arrayListOf(),
        @JsonProperty("next_episode_id"           ) var nextEpisodeId           : String?                 = null,
        @JsonProperty("upload_date"               ) var uploadDate              : String?                 = null,
        @JsonProperty("playback"                  ) var playback                : String?                 = null,
        @JsonProperty("available_offline"         ) var availableOffline        : Boolean?                = null,
        @JsonProperty("streams_link"              ) var streamsLink             : String?                 = null,
        @JsonProperty("seo_title"                 ) var seoTitle                : String?                 = null,
        @JsonProperty("closed_captions_available" ) var closedCaptionsAvailable : Boolean?                = null,
        @JsonProperty("premium_available_date"    ) var premiumAvailableDate    : String?                 = null,
        @JsonProperty("episode_air_date"          ) var episodeAirDate          : String?                 = null,
        @JsonProperty("versions"                  ) var versions                : ArrayList<Versions>?                 = null,
        @JsonProperty("episode"                   ) var episode                 : String?                 = null,
        @JsonProperty("images"                    ) var images                  : KrunchyImages?                 = KrunchyImages(),
        @JsonProperty("description"               ) var description             : String?                 = null,
        @JsonProperty("production_episode_id"     ) var productionEpisodeId     : String?                 = null,
        @JsonProperty("series_id"                 ) var seriesId                : String?                 = null,
        @JsonProperty("is_clip"                   ) var isClip                  : Boolean?                = null,
        @JsonProperty("audio_locale"              ) var audioLocale             : String?                 = null,
        @JsonProperty("availability_starts"       ) var availabilityStarts      : String?                 = null,
        @JsonProperty("subtitle_locales"          ) var subtitleLocales         : ArrayList<String>       = arrayListOf(),
        @JsonProperty("season_slug_title"         ) var seasonSlugTitle         : String?                 = null,
        @JsonProperty("series_title"              ) var seriesTitle             : String?                 = null,
        @JsonProperty("next_episode_title"        ) var nextEpisodeTitle        : String?                 = null,
        @JsonProperty("listing_id"                ) var listingId               : String?                 = null,
        @JsonProperty("seo_description"           ) var seoDescription          : String?                 = null,
        @JsonProperty("available_date"            ) var availableDate           : String?                 = null,
        @JsonProperty("is_premium_only"           ) var isPremiumOnly           : Boolean?                = null,
        @JsonProperty("is_dubbed"                 ) var isDubbed                : Boolean?                = null,
        @JsonProperty("availability_notes"        ) var availabilityNotes       : String?                 = null,
        @JsonProperty("season_number"             ) var seasonNumber            : Int?                    = null,
        @JsonProperty("media_type"                ) var mediaType               : String?                 = null,
        @JsonProperty("season_title"              ) var seasonTitle             : String?                 = null,
        @JsonProperty("identifier"                ) var identifier              : String?                 = null,
        @JsonProperty("premium_date"              ) var premiumDate             : String?                 = null,
        @JsonProperty("eligible_region"           ) var eligibleRegion          : String?                 = null,
        @JsonProperty("id"                        ) var id                      : String?                 = null,
        @JsonProperty("is_mature"                 ) var isMature                : Boolean?                = null,
        @JsonProperty("is_subbed"                 ) var isSubbed                : Boolean?                = null,
        @JsonProperty("episode_number"            ) var episodeNumber           : Int?                    = null,
        @JsonProperty("channel_id"                ) var channelId               : String?                 = null,
        @JsonProperty("slug_title"                ) var slugTitle               : String?                 = null,
        @JsonProperty("mature_blocked"            ) var matureBlocked           : Boolean?                = null,
        @JsonProperty("season_tags"               ) var seasonTags              : ArrayList<String>       = arrayListOf(),
        @JsonProperty("slug"                      ) var slug                    : String?                 = null,
        @JsonProperty("hd_flag"                   ) var hdFlag                  : Boolean?                = null,
        @JsonProperty("duration_ms"               ) var durationMs              : Int?                    = null,
        @JsonProperty("title"                     ) var title                   : String?                 = null,
        @JsonProperty("availability_ends"         ) var availabilityEnds        : String?                 = null,
        @JsonProperty("series_slug_title"         ) var seriesSlugTitle         : String?                 = null,
        @JsonProperty("sequence_number"           ) var sequenceNumber          : Int?                    = null,
        @JsonProperty("season_id"                 ) var seasonId                : String?                 = null,
        @JsonProperty("free_available_date"       ) var freeAvailableDate       : String?                 = null,
        @JsonProperty("is_simulcast"             ) var isSimulcast            : Boolean?                = null,
        @JsonProperty("season_sequence_number"   ) var seasonSequenceNumber   : Int?                    = null,
        @JsonProperty("season_display_number"    ) var seasonDisplayNumber    : String?                 = null,
        @JsonProperty("number_of_episodes"       ) var numberOfEpisodes       : Int?                    = null,
        @JsonProperty("audio_locales"            ) var audioLocales           : ArrayList<String>       = arrayListOf(),
        @JsonProperty("is_complete"              ) var isComplete             : Boolean?                = null,
        @JsonProperty("keywords"                 ) var keywords               : ArrayList<String>       = arrayListOf(),
    )


    data class Versions (
        @JsonProperty("audio_locale" ) var audioLocale : String?  = null,
        @JsonProperty("guid"         ) var guid        : String?  = null,
        @JsonProperty("original"     ) var original    : Boolean? = null,
        @JsonProperty("variant"      ) var variant     : String?  = null
    )


    data class LoadDataInfo (
        @JsonProperty("tvtype"   ) var tvtype   : String? = null,
        @JsonProperty("seriesID" ) var seriesID : String? = null
    )


    private suspend fun getMovie(id: String?):ArrayList<Episode> {
        getKrunchyToken()
        val movie = ArrayList<Episode>()
        val metadainfo = app.get("$krunchyapi/content/v2/cms/movie_listings/$id/movies?locale=en-US", headers = latestKrunchyHeader).parsed<KrunchyLoadMain>()
        metadainfo.data.map {
            val title = it.title
            val epID = it.id
            val epdesc = it.description
            val posterstring = it.images?.thumbnail.toString()
            val posterRegex = Regex("PosterTall\\(height=338,.*source=(.*),.*type=thumbnail,.width=600\\)")
            val poster = posterRegex.find(posterstring)?.destructured?.component1()
            movie.add(
                Episode(
                    name = title,
                    data = epID!!,
                    description = epdesc,
                    posterUrl = poster
                ))
        }
        return movie
    }
    override suspend fun load(url: String): LoadResponse {
        getKrunchyToken()
        val fixedData = url.replace("https://consumet.org/","")
        val parseData = parseJson<LoadDataInfo>(fixedData)
        val seriesIDSuper = parseData.seriesID
        val tvtype = if (parseData.tvtype!!.contains("movie")) TvType.AnimeMovie else TvType.Anime
        val tvTypeAnime = tvtype == TvType.Anime
        val metadataUrl = if (tvTypeAnime) "$krunchyapi/content/v2/cms/series/$seriesIDSuper?&locale=en-US"
        else "$krunchyapi/content/v2/cms/movie_listings/$seriesIDSuper?locale=en-US"
        val metadainfo = app.get(metadataUrl, headers = latestKrunchyHeader).parsed<KrunchyLoadMain>()
        val title = metadainfo.data.first().title
        val tags = metadainfo.data.first().keywords
        val year = metadainfo.data.first().seriesLaunchYear
        val posterstring = metadainfo.data.first().images?.posterWide?.toString()
        val posterstring2 = metadainfo.data.first().images?.posterTall.toString()
        val posterregex = Regex("height=900.*source=(.*),.*type=poster_wide.*PosterTall")
        val posterRegex2 = Regex("height=1800.*source=(.*),.*type=poster_tall.*PosterTall")
        val backgroundposter = posterregex.find(posterstring!!)?.destructured?.component1() ?: ""
        val poster = posterRegex2.find(posterstring2)?.destructured?.component1() ?: ""
        val description = metadainfo.data.first().description

        return newAnimeLoadResponse(title!!, seriesIDSuper!!, tvtype){
            if (tvTypeAnime) {
                val krunchyDUB = getKrunchyDubEpisodes(seriesIDSuper)
                val krunchySUB = getKrunchySubEpisodes(seriesIDSuper)
                addEpisodes(DubStatus.Subbed,krunchySUB.distinct().toList())
                addEpisodes(DubStatus.Dubbed,krunchyDUB.distinct().toList())
            } else {
                addEpisodes(DubStatus.Subbed, getMovie(seriesIDSuper))
            }
            this.plot = description
            this.tags = tags
            this.year = year
            this.posterUrl = poster
            this.backgroundPosterUrl = backgroundposter
        }
    }

    suspend fun getKamyStream(
        streamLink: String,
        name: String,
        callback: (ExtractorLink) -> Unit
    )  {
        return generateM3u8(
            this.name,
            streamLink,
            "https://static.crunchyroll.com/"
        ).forEach { sub ->
            callback(
                ExtractorLink(
                    this.name,
                    name,
                    sub.url,
                    "https://static.crunchyroll.com/",
                    getQualityFromName(sub.quality.toString()),
                    true
                )
            )
        }
    }


    data class ConsumetSubtitles(
        @JsonProperty("url") val url: String? = null,
        @JsonProperty("lang") val lang: String? = null,
    )

    data class ConsumetSources(
        @JsonProperty("url") val url: String? = null,
        @JsonProperty("quality") val quality: String? = null,
        @JsonProperty("isM3U8") val isM3U8: Boolean? = null,
    )
    data class ConsumetSourcesMain(
        @JsonProperty("sources") val sources: ArrayList<ConsumetSources>? = arrayListOf(),
        @JsonProperty("subtitles") val subtitles: ArrayList<ConsumetSubtitles>? = arrayListOf(),
    )

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val response = app.get("$kronchyConsumetapi/watch?episodeId=$data").parsed<ConsumetSourcesMain>()
        response.sources?.map {
            val quality = it.quality
            val hardsub = quality == "hardsub"
            val auto = quality == "auto"
            val name = if (hardsub) "Kronch Hardsub English (US)" else "Kronch RAW"
            if (hardsub) {
                getKamyStream(it.url!!, name, callback)
            }
            if (auto) {
                getKamyStream(it.url!!, name, callback)
            }
        }
        response.subtitles?.map {
            val lang= when (it.lang){
                "ja-JP" -> "Japanese"
                "en-US" -> "English"
                "de-DE" -> "German"
                "es-ES" -> "Spanish"
                "es-419" -> "Spanish 2"
                "fr-FR" -> "French"
                "it-IT" -> "Italian"
                "pt-BR" -> "Portuguese (Brazil)"
                "pt-PT" -> "Portuguese (Portugal)"
                "ru-RU" -> "Russian"
                "zh-CN" -> "Chinese (Simplified)"
                "tr-TR" -> "Turkish"
                "ar-ME" -> "Arabic"
                "ar-SA" -> "Arabic (Saudi Arabia)"
                "uk-UK" -> "Ukrainian"
                "he-IL" -> "Hebrew"
                "pl-PL" -> "Polish"
                "ro-RO" -> "Romanian"
                "sv-SE" -> "Swedish"
                ""      -> ""
                else -> "[${it.lang}] "
            }
            val url = it.url
            subtitleCallback.invoke(
                SubtitleFile(lang,url!!)
            )
        }
        return true
    }
}