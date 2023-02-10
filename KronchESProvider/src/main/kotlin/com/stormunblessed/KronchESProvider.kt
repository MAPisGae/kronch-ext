package com.stormunblessed

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.M3u8Helper
import com.lagradost.cloudstream3.utils.getQualityFromName

class KronchESProvider: MainAPI() {

    companion object {
        //var latestHeader: Map<String, String> = emptyMap()
        var latestKrunchyHeader: Map<String, String> = emptyMap()
        //var latestKrunchySession: Map<String, String> = emptyMap()
        var latestcountryID = ""
        private const val krunchyapi = "https://beta-api.crunchyroll.com"
        private const val kronchyConsumetapi = "https://api.consumet.org/anime/crunchyroll"

    }

    override var name = "Kronch ES"
    override var mainUrl = "https://www.crunchyroll.com"
    override val instantLinkLoading = false
    override val hasMainPage = true
    override var lang = "es"
    override val supportedTypes = setOf(
        TvType.AnimeMovie,
        TvType.Anime,
    )


    data class KronchyToken (
        @JsonProperty("access_token" ) val accessToken : String? = null,
        @JsonProperty("expires_in"   ) val expiresIn   : Int?    = null,
        @JsonProperty("token_type"   ) val tokenType   : String? = null,
        @JsonProperty("scope"        ) val scope       : String? = null,
        @JsonProperty("country"      ) val country     : String? = null
    )

    private suspend fun getKronchToken(): Map<String, String> {
        val testingasa = app.post("$krunchyapi/auth/v1/token",
            headers = mapOf(
                "User-Agent"  to "Crunchyroll/3.26.1 Android/11 okhttp/4.9.2",
                "Content-Type" to "application/x-www-form-urlencoded",
                "Authorization" to "Basic aHJobzlxM2F3dnNrMjJ1LXRzNWE6cHROOURteXRBU2Z6QjZvbXVsSzh6cUxzYTczVE1TY1k="
            ),
            data = mapOf("grant_type" to "client_id")
        ).parsed<KronchyToken>()
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





    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val items = ArrayList<HomePageList>()
        val urls = listOf(
            Pair("$krunchyapi/content/v1/browse?locale=es-ES&n=20&sort_by=popularity", "Popular"),
            Pair("$krunchyapi/content/v1/browse?locale=es-ES&n=20&sort_by=newly_added", "Recientes")
        )
        getKronchToken()

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

    data class ConsumetSearch (
        @JsonProperty("results"     ) var results     : ArrayList<ConsumetSearchResults> = arrayListOf()
    )

    data class ConsumetSearchResults (
        @JsonProperty("id"          ) var id          : String?  = null,
        @JsonProperty("title"       ) var title       : String?  = null,
        @JsonProperty("type"        ) var type        : String?  = null,
        @JsonProperty("description" ) var description : String?  = null,
        @JsonProperty("image"       ) var image       : String?  = null,
        @JsonProperty("cover"       ) var cover       : String?  = null,
        @JsonProperty("hasDub"      ) var hasDub      : Boolean? = null,
        @JsonProperty("hasSub"      ) var hasSub      : Boolean? = null
    )

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$kronchyConsumetapi/$query"
        val search = ArrayList<SearchResponse>()
        val rep = app.get(url).parsed<ConsumetSearch>()
        rep.results.map { info ->
            val title = info.title
            val image = info.image
            val dubExist = info.hasDub == true
            val subExist = info.hasSub == true
            val id = info.id
            val type = info.type
            val data = "{\"tvtype\":\"$type\",\"seriesID\":\"$id\"}"
            search.add(newAnimeSearchResponse(title!!, data) {
                this.posterUrl = image
                addDubStatus(dubExist, subExist)
            })
        }
        return search
    }


    data class LoadDataInfo (
        @JsonProperty("tvtype"   ) var tvtype   : String? = null,
        @JsonProperty("seriesID" ) var seriesID : String? = null
    )


    data class ConsumetMetadata (
        @JsonProperty("id"              ) var id              : String?                    = null,
        @JsonProperty("title"           ) var title           : String?                    = null,
        @JsonProperty("isAdult"         ) var isAdult         : Boolean?                   = null,
        @JsonProperty("image"           ) var image           : String?                    = null,
        @JsonProperty("cover"           ) var cover           : String?                    = null,
        @JsonProperty("description"     ) var description     : String?                    = null,
        @JsonProperty("releaseDate"     ) var releaseDate     : Int?                       = null,
        @JsonProperty("genres"          ) var genres          : ArrayList<String>          = arrayListOf(),
        @JsonProperty("season"          ) var season          : String?                    = null,
        @JsonProperty("hasDub"          ) var hasDub          : Boolean?                   = null,
        @JsonProperty("hasSub"          ) var hasSub          : Boolean?                   = null,
        @JsonProperty("rating"          ) var rating          : String?                    = null,
        @JsonProperty("ratingTotal"     ) var ratingTotal     : Int?                       = null,
        @JsonProperty("recommendations" ) var recommendations : ArrayList<ConsumetRecommendations> = arrayListOf(),
        @JsonProperty("episodes" )val episodes: Map<String, List<EpisodesData>>? = null
    )


    data class ConsumetRecommendations (
        @JsonProperty("id"          ) var id          : String? = null,
        @JsonProperty("title"       ) var title       : String? = null,
        @JsonProperty("image"       ) var image       : String? = null,
        @JsonProperty("cover"       ) var cover       : String? = null,
        @JsonProperty("description" ) var description : String? = null
    )

    data class EpisodesData (
        @JsonProperty("id"             ) var id            : String?  = null,
        @JsonProperty("season_number"  ) var seasonNumber  : Int?     = null,
        @JsonProperty("episode_number" ) var episodeNumber : Int?     = null,
        @JsonProperty("title"          ) var title         : String?  = null,
        @JsonProperty("image"          ) var image         : String?  = null,
        @JsonProperty("description"    ) var description   : String?  = null,
        @JsonProperty("releaseDate"    ) var releaseDate   : String?  = null,
        @JsonProperty("isHD"           ) var isHD          : Boolean? = null,
        @JsonProperty("isAdult"        ) var isAdult       : Boolean? = null,
        @JsonProperty("isDubbed"       ) var isDubbed      : Boolean? = null,
        @JsonProperty("isSubbed"       ) var isSubbed      : Boolean? = null,
        @JsonProperty("maturity_ratings"          ) var maturityRatings         : ArrayList<String>       = arrayListOf(),
        @JsonProperty("next_episode_id"           ) var nextEpisodeId           : String?                 = null,
        @JsonProperty("upload_date"               ) var uploadDate              : String?                 = null,
        @JsonProperty("playback"                  ) var playback                : String?                 = null,
        @JsonProperty("available_offline"         ) var availableOffline        : Boolean?                = null,
        @JsonProperty("streams_link"              ) var streamsLink             : String?                 = null,
        @JsonProperty("seo_title"                 ) var seoTitle                : String?                 = null,
        @JsonProperty("premium_available_date"    ) var premiumAvailableDate    : String?                 = null,
        @JsonProperty("episode_air_date"          ) var episodeAirDate          : String?                 = null,
        @JsonProperty("episode"                   ) var episode                 : String?                 = null,
        @JsonProperty("images"                    ) var images                  : KrunchyImages?                 = KrunchyImages(),
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
        @JsonProperty("availability_notes"        ) var availabilityNotes       : String?                 = null,
        @JsonProperty("media_type"                ) var mediaType               : String?                 = null,
        @JsonProperty("season_title"              ) var seasonTitle             : String?                 = null,
        @JsonProperty("identifier"                ) var identifier              : String?                 = null,
        @JsonProperty("premium_date"              ) var premiumDate             : String?                 = null,
        @JsonProperty("eligible_region"           ) var eligibleRegion          : String?                 = null,
        @JsonProperty("is_mature"                 ) var isMature                : Boolean?                = null,
        @JsonProperty("channel_id"                ) var channelId               : String?                 = null,
        @JsonProperty("slug_title"                ) var slugTitle               : String?                 = null,
        @JsonProperty("mature_blocked"            ) var matureBlocked           : Boolean?                = null,
        @JsonProperty("season_tags"               ) var seasonTags              : ArrayList<String>       = arrayListOf(),
        @JsonProperty("slug"                      ) var slug                    : String?                 = null,
        @JsonProperty("hd_flag"                   ) var hdFlag                  : Boolean?                = null,
        @JsonProperty("duration_ms"               ) var durationMs              : Int?                    = null,
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

    data class KrunchySeasonsInfo (
        @JsonProperty("total" ) var total : Int?            = null,
        @JsonProperty("data"  ) var data  : ArrayList<EpisodesData> = arrayListOf(),
    )

    /* data class Versions (
         @JsonProperty("audio_locale" ) var audioLocale : String?  = null,
         @JsonProperty("guid"         ) var guid        : String?  = null,
         @JsonProperty("original"     ) var original    : Boolean? = null,
         @JsonProperty("variant"      ) var variant     : String?  = null
     ) */


    private suspend fun getKronchSeasonsInfo(fixedID: String?, type: String?): ConsumetMetadata {
        val url = "$kronchyConsumetapi/info?id=$fixedID&mediaType=$type&fetchAllSeasons=true"
        val responsText = app.get(url).text
        return parseJson(responsText)
    }
    private fun getepisode(data: EpisodesData?, isSubbed: Boolean?):Episode{
        val eptitle = data?.title
        val epID = data?.id
        val epplot = data?.description
        val season = data?.seasonNumber
        val epnum = data?.episodeNumber
        val posterstring = data?.images?.thumbnail.toString()
        val posterRegex = Regex("PosterTall\\(height=338,.*source=(.*),.*type=thumbnail,.width=600\\)")
        val dataep = "{\"id\":\"$epID\",\"issub\":$isSubbed}"
        val poster = posterRegex.find(posterstring)?.destructured?.component1()
        val epthumb = data?.image ?: poster
        return Episode(
            dataep,
            eptitle!!,
            season,
            epnum,
            epthumb,
            //description = epplot
        )
    }


    private suspend fun getMovie(id: String?):ArrayList<Episode> {
        getKronchToken()
        val movie = ArrayList<Episode>()
        val metadainfo = app.get("$krunchyapi/content/v2/cms/movie_listings/$id/movies?locale=es-ES", headers = latestKrunchyHeader).parsed<KrunchyLoadMain>()
        metadainfo.data.map {
            val title = it.title
            val epID = it.id
            val epdesc = it.description
            val posterstring = it.images?.thumbnail.toString()
            val issub = it.isSubbed
            val posterRegex = Regex("PosterTall\\(height=338,.*source=(.*),.*type=thumbnail,.width=600\\)")
            val poster = posterRegex.find(posterstring)?.destructured?.component1()
            val dataep = "{\"id\":\"$epID\",\"issub\":$issub}"
            movie.add(
                Episode(
                    name = title,
                    data = dataep,
                    description = epdesc,
                    posterUrl = poster,

                    ))
        }
        return movie
    }

    override suspend fun load(url: String): LoadResponse {
        val fixedData = url.replace("https://www.crunchyroll.com/","")
        val parsedData = parseJson<LoadDataInfo>(fixedData)
        val seriesIDSuper = parsedData.seriesID
        val type = parsedData.tvtype
        val tvType = if (type!!.contains("movie")) TvType.AnimeMovie else TvType.Anime
        val isMovie = tvType == TvType.AnimeMovie
        //val urldata = if (isMovie) "$mainUrl/watch/$seriesIDSuper" else "$mainUrl/series/$seriesIDSuper"
        val dubEps = ArrayList<Episode>()
        val subEps = ArrayList<Episode>()
        var title = ""
        var plot = ""
        var tags = listOf("")
        var poster = ""
        var year = "".toIntOrNull()
        var backposter = ""
        val infodata = "{\"tvtype\":\"$type\",\"seriesID\":\"$seriesIDSuper\"}"
        //val recommendations = ArrayList<SearchResponse>()
        if (!isMovie) {
            val response = getKronchSeasonsInfo(seriesIDSuper, "series")
            getKronchToken()
            title = response.title.toString()
            plot = response.description.toString()
            year = response.releaseDate
            poster = response.image.toString()
            backposter = response.cover.toString()
            tags = response.genres

            /*
            Removed cause its bugged
            response.recommendations.map {
                 val name = it.title
                 val img = it.image
                 val id = it.id
                 val datainfo = "{\"tvtype\":\"series\",\"seriesID\":\"$id\"}"
                 recommendations.add(
                     newAnimeSearchResponse(name!!, datainfo){
                         this.posterUrl = img
                     })
             } */

            val subJson = response.episodes?.filter {
                it.key.contains("subbed", ignoreCase = true)
            }
            val dubJson = response.episodes?.filter {
                it.key.contains("Spanish")
            }

            subJson?.map {
                val key = it.key.contains("subbed", ignoreCase = true)
                if (key) {
                    it.value.map {
                        subEps.add(getepisode(it, true))
                    }
                }
            }
            dubJson?.map {
                val key = it.key.contains("Spanish")
                if (key) {
                    it.value.map {
                        dubEps.add(getepisode(it, false))
                    }
                }
            }
            if (title == "One Piece") {
                val resptwo = app.get("$krunchyapi/content/v2/cms/series/$seriesIDSuper/seasons?locale=es-ES", headers = latestKrunchyHeader).parsed<KrunchySeasonsInfo>()
                val subs2 = resptwo.data.filter {
                    !it.seriesId.isNullOrEmpty() || it.audioLocale == "ja-JP" || it.audioLocale == "zh-CN"
                            || !it.title!!.contains(Regex("Piece: East Blue|Piece: Alabasta|Piece: Sky Island"))
                }
                subs2.apmap {
                    val seriesID = it.id
                    val sss = app.get("$krunchyapi/content/v2/cms/seasons/$seriesID/episodes?locale=es-ES", headers = latestKrunchyHeader).parsed<KrunchySeasonsInfo>()
                    sss.data.map {
                        subEps.add(getepisode(it, true))
                    }
                }
            }
        }
        if (isMovie) {
            getKronchToken()
            val moviedataurl = "$krunchyapi/content/v2/cms/movie_listings/$seriesIDSuper?locale=es-ES"
            val resp = app.get(moviedataurl, headers = latestKrunchyHeader).parsed<KrunchyLoadMain>().data.first()
            val posterstring = resp.images?.posterWide?.toString()
            val posterstring2 = resp.images?.posterTall.toString()
            val posterregex = Regex("height=900.*source=(.*),.*type=poster_wide.*PosterTall")
            val posterRegex2 = Regex("height=1800.*source=(.*),.*type=poster_tall.*PosterTall")
            title = resp.title.toString()
            backposter = posterregex.find(posterstring!!)?.destructured?.component1() ?: ""
            poster = posterRegex2.find(posterstring2)?.destructured?.component1() ?: ""
            plot = resp.description.toString()
            tags = resp.keywords
            year = resp.seriesLaunchYear
        }
        return newAnimeLoadResponse(title, infodata, TvType.Anime) {
            if (subEps.isNotEmpty()) addEpisodes(DubStatus.Subbed,subEps)
            if (dubEps.isNotEmpty()) addEpisodes(DubStatus.Dubbed,dubEps)
            if (isMovie) addEpisodes(DubStatus.Subbed, getMovie(seriesIDSuper))
            this.plot = plot
            this.tags = tags
            this.year = year
            this.posterUrl = poster
            this.backgroundPosterUrl = backposter
            //this.recommendations = recommendations
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

    data class EpsInfo (
        @JsonProperty("id"    ) var id    : String?  = null,
        @JsonProperty("issub" ) var issub : Boolean? = null
    )


    private suspend fun getKronchStream(
        streamLink: String,
        name: String,
        callback: (ExtractorLink) -> Unit
    )  {
        return M3u8Helper.generateM3u8(
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

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val parsedata = parseJson<EpsInfo>(data)
        val response = app.get("$kronchyConsumetapi/watch?episodeId=${parsedata.id}").parsed<ConsumetSourcesMain>()

        response.sources?.apmap {
            val quality = it.quality
            val issub = parsedata.issub == true
            val hardsub = quality!!.contains(Regex("es-ES|es-419"))
            val auto = quality == "auto"
            val name = if (hardsub && issub && quality.contains("es-ES")) "Kronch Hardsub Español España"
            else if (hardsub && issub && quality.contains("es-419")) "Kronch Hardsub Español LAT"
            else if (!issub) "Kronch Español"
            else "Kronch RAW"
            if (hardsub && issub) {
                getKronchStream(it.url!!, name, callback)
            }
            if (auto) {
                getKronchStream(it.url!!, name, callback)
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