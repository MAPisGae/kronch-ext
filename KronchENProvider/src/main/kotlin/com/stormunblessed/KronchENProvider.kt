package com.stormunblessed

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.M3u8Helper
import com.lagradost.cloudstream3.utils.getQualityFromName

class KronchENProvider: MainAPI() {

    companion object {
        //var latestHeader: Map<String, String> = emptyMap()
        var latestKrunchyHeader: Map<String, String> = emptyMap()
        //var latestKrunchySession: Map<String, String> = emptyMap()
        var latestcountryID = ""
        private const val krunchyapi = "https://beta-api.crunchyroll.com"
        private const val kronchyConsumetapi = "https://api.consumet.org/anime/crunchyroll"

    }

    override var name = "Kronch"
    override var mainUrl = "https://www.crunchyroll.com"
    override val instantLinkLoading = false
    override val hasMainPage = true
    override var lang = "en"
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


    data class ConsuToken (
        @JsonProperty("access_token" ) var accessToken : String? = null,
        @JsonProperty("expires_in"   ) var expiresIn   : Int?    = null,
        @JsonProperty("token_type"   ) var tokenType   : String? = null,
        @JsonProperty("scope"        ) var scope       : String? = null,
        @JsonProperty("country"      ) var country     : String? = null,
        @JsonProperty("account_id"   ) var accountId   : String? = null,
        @JsonProperty("signature"    ) var signature   : String? = null,
        @JsonProperty("key_pair_id"  ) var keyPairId   : String? = null,
        @JsonProperty("bucket"       ) var bucket      : String? = null,
        @JsonProperty("policy"       ) var policy      : String? = null
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

    private suspend fun getConsuToken():Map<String, String> {
        val consuToken = app.get("https://cronchy.consumet.stream/token").parsed<ConsuToken>()
        val header = mapOf(
            "Authorization" to "${consuToken.tokenType} ${consuToken.accessToken}"
        )
        latestKrunchyHeader = header
        latestcountryID = consuToken.country!!
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
            Pair("$krunchyapi/content/v1/browse?locale=en-US&n=30&sort_by=popularity", "Popular"),
            Pair("$krunchyapi/content/v1/browse?locale=en-US&n=30&sort_by=newly_added", "Newly Added")
        )
        getKronchToken()

        urls.apmap {(url, name) ->
            val res = app.get(url,
                headers = latestKrunchyHeader
            ).parsed<KrunchyHome>()
            val home = res.items.map {
                val title = it.title
                val issub = it.seriesMetadata?.isSubbed == true
                val isdub = it.seriesMetadata?.isDubbed == true
                //val epss = it.seriesMetadata?.episodeCount
                val posterstring = it.images?.posterTall.toString()
                //val ttt = it.images?.posterTall?.get(0)?.get(6)?.source ?: ""
                val posterregex = Regex("height=2340.*source=(.*),.*type=poster_tall")
                val poster = posterregex.find(posterstring)?.destructured?.component1() ?: ""
                val seriesID = it.id
                val data = "{\"tvtype\":\"${it.type}\",\"seriesID\":\"$seriesID\"}"
                newAnimeSearchResponse(title!!, data){
                    this.posterUrl = poster
                    addDubStatus(isdub, issub)
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
    data class Versions (
        @JsonProperty("audio_locale" ) var audioLocale : String?  = null,
        @JsonProperty("guid"         ) var guid        : String?  = null,
        @JsonProperty("original"     ) var original    : Boolean? = null,
        @JsonProperty("variant"      ) var variant     : String?  = null
    )


    private suspend fun getKronchMetadataInfo(url: String): KrunchyLoadMain {
        getConsuToken()
        return app.get(url, headers = latestKrunchyHeader).parsed()
    }
    private fun getEpisode(data: BetaKronchData?, isSubbed: Boolean?):Episode{
        val eptitle = data?.title
        val epID= data?.streamsLink?.substringAfter("/videos/")?.substringBefore("/streams")
        val epthumb = data?.images?.thumbnail?.map { it[3].source }?.first() ?: ""
        val epplot = data?.description
        val season = data?.seasonNumber
        val epnum = data?.episodeNumber
        val dataep = "{\"id\":\"$epID\",\"issub\":$isSubbed}"
        val aaseason = "$season-$epnum"
        val seasonid =aaseason.let { str ->
            str.split("-").mapNotNull { subStr -> subStr.toIntOrNull() }
        }.sorted()
        val isValid = seasonid.size == 2
        val aaepisode = if (isValid) seasonid.getOrNull(1) else null
        val aaseasontwo = if (isValid) seasonid.getOrNull(0) else null
        return Episode(
            dataep,
            eptitle!!,
            season = aaseasontwo,
            episode = aaepisode,
            posterUrl = epthumb,
            description = epplot
        )
    }


    private suspend fun getMovie(id: String?):ArrayList<Episode> {
        getKronchToken()
        val movie = ArrayList<Episode>()
        val metadainfo = app.get("$krunchyapi/content/v2/cms/movie_listings/$id/movies?locale=en-US", headers = latestKrunchyHeader).parsed<KrunchyLoadMain>()
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


    data class  BetaKronch(
        @JsonProperty("total" ) var total : Int?            = null,
        @JsonProperty("data"  ) var data  : ArrayList<BetaKronchData> = arrayListOf(),
    )
    data class BetaKronchData (
        @JsonProperty("is_complete"              ) var isComplete             : Boolean?                = null,
        @JsonProperty("keywords"                 ) var keywords               : ArrayList<String>?       = arrayListOf(),
        @JsonProperty("maturity_ratings"         ) var maturityRatings        : ArrayList<String>?       = arrayListOf(),
        @JsonProperty("is_subbed"                ) var isSubbed               : Boolean?                = null,
        @JsonProperty("audio_locale"             ) var audioLocale            : String?                 = null,
        @JsonProperty("id"                       ) var id                     : String?                 = null,
        @JsonProperty("title"                    ) var title                  : String?                 = null,
        @JsonProperty("series_id"                ) var seriesId               : String?                 = null,
        @JsonProperty("is_simulcast"             ) var isSimulcast            : Boolean?                = null,
        @JsonProperty("slug_title"               ) var slugTitle              : String?                 = null,
        @JsonProperty("season_number"            ) var seasonNumber           : Int?                    = null,
        @JsonProperty("versions"                 ) var versions               : ArrayList<Versions>?    = arrayListOf(),
        @JsonProperty("free_available_date"       ) var freeAvailableDate       : String?                 = null,
        @JsonProperty("next_episode_id"           ) var nextEpisodeId           : String?                 = null,
        @JsonProperty("premium_date"              ) var premiumDate             : String?                 = null,
        @JsonProperty("episode_number"            ) var episodeNumber           : Int?                    = null,
        @JsonProperty("season_title"              ) var seasonTitle             : String?                 = null,
        @JsonProperty("is_mature"                 ) var isMature                : Boolean?                = null,
        @JsonProperty("streams_link"              ) var streamsLink             : String?                 = null,
        @JsonProperty("available_date"            ) var availableDate           : String?                 = null,
        @JsonProperty("listing_id"                ) var listingId               : String?                 = null,
        @JsonProperty("is_dubbed"                 ) var isDubbed                : Boolean?                = null,
        @JsonProperty("slug"                      ) var slug                    : String?                 = null,
        @JsonProperty("mature_blocked"            ) var matureBlocked           : Boolean?                = null,
        @JsonProperty("sequence_number"           ) var sequenceNumber          : Int?                    = null,
        @JsonProperty("is_clip"                   ) var isClip                  : Boolean?                = null,
        @JsonProperty("season_slug_title"         ) var seasonSlugTitle         : String?                 = null,
        @JsonProperty("description"               ) var description             : String?                 = null,
        @JsonProperty("eligible_region"           ) var eligibleRegion          : String?                 = null,
        @JsonProperty("media_type"                ) var mediaType               : String?                 = null,
        @JsonProperty("subtitle_locales"          ) var subtitleLocales         : ArrayList<String>?       = arrayListOf(),
        @JsonProperty("episode"                   ) var episode                 : String?                 = null,
        @JsonProperty("images"                    ) var images                  : KrunchyImages?                 = KrunchyImages(),
        @JsonProperty("series_title"              ) var seriesTitle             : String?                 = null,
        @JsonProperty("next_episode_title"        ) var nextEpisodeTitle        : String?                 = null,
        @JsonProperty("seo_title"                 ) var seoTitle                : String?                 = null,
        @JsonProperty("hd_flag"                   ) var hdFlag                  : Boolean?                = null,
        @JsonProperty("available_offline"         ) var availableOffline        : Boolean?                = null,
        @JsonProperty("series_slug_title"         ) var seriesSlugTitle         : String?                 = null,
        @JsonProperty("availability_ends"         ) var availabilityEnds        : String?                 = null,
        @JsonProperty("production_episode_id"     ) var productionEpisodeId     : String?                 = null,
        @JsonProperty("identifier"                ) var identifier              : String?                 = null,
        @JsonProperty("episode_air_date"          ) var episodeAirDate          : String?                 = null,
        @JsonProperty("upload_date"               ) var uploadDate              : String?                 = null,
        @JsonProperty("availability_starts"       ) var availabilityStarts      : String?                 = null,
        @JsonProperty("premium_available_date"    ) var premiumAvailableDate    : String?                 = null,
        @JsonProperty("availability_notes"        ) var availabilityNotes       : String?                 = null,
        @JsonProperty("season_id"                 ) var seasonId                : String?                 = null,
        @JsonProperty("duration_ms"               ) var durationMs              : Int?                    = null,
        @JsonProperty("closed_captions_available" ) var closedCaptionsAvailable : Boolean?                = null,
        @JsonProperty("seo_description"           ) var seoDescription          : String?                 = null,
        @JsonProperty("is_premium_only"           ) var isPremiumOnly           : Boolean?                = null
    )


    data class BetaKronchRecsMain (
        @JsonProperty("total" ) var total : Int?            = null,
        @JsonProperty("data"  ) var data  : ArrayList<BetaKronchRecData> = arrayListOf(),
    )
    data class BetaKronchRecData (
        @JsonProperty("id"                  ) var id                : String?         = null,
        @JsonProperty("slug_title"          ) var slugTitle         : String?         = null,
        @JsonProperty("title"               ) var title             : String?         = null,
        @JsonProperty("promo_title"         ) var promoTitle        : String?         = null,
        @JsonProperty("description"         ) var description       : String?         = null,
        @JsonProperty("type"                ) var type              : String?         = null,
        @JsonProperty("images"              ) var images            : KrunchyImages?         = KrunchyImages(),
        @JsonProperty("promo_description"   ) var promoDescription  : String?         = null,
        @JsonProperty("new"                 ) var new               : Boolean?        = null,
        @JsonProperty("slug"                ) var slug              : String?         = null,
        @JsonProperty("channel_id"          ) var channelId         : String?         = null,
        @JsonProperty("linked_resource_key" ) var linkedResourceKey : String?         = null,
        @JsonProperty("external_id"         ) var externalId        : String?         = null,
    )


    private suspend fun getRecommendations(seriesId: String?): List<SearchResponse>?{
        getConsuToken()
        val recsurl = "$krunchyapi/content/v2/discover/similar_to/$seriesId?locale=en-US&n=30"
        val res = app.get(recsurl, headers = latestKrunchyHeader).parsedSafe<BetaKronchRecsMain>()
        return res?.data?.map { rec ->
            val sID = rec.id
            val rTitle = rec.title ?: ""
            val poster = rec.images?.posterTall?.map { it[5].source }?.first() ?: ""
            val rType = rec.type
            val rdata = "{\"tvtype\":\"$rType\",\"seriesID\":\"$sID\"}"
            newAnimeSearchResponse(rTitle,rdata){
                this.posterUrl = poster
            }
        }
    }
    override suspend fun load(url: String): LoadResponse {
        val fixedData = url.replace("https://www.crunchyroll.com/","")
        val parsedData = parseJson<LoadDataInfo>(fixedData)
        val seriesIDSuper = parsedData.seriesID
        val type = parsedData.tvtype
        val tvType = if (type!!.contains("movie")) TvType.AnimeMovie else TvType.Anime
        val isMovie = tvType == TvType.AnimeMovie
        val tttt = if (isMovie) "$krunchyapi/content/v2/cms/movie_listings/$seriesIDSuper?locale=en-US" else "$krunchyapi/content/v2/cms/series/$seriesIDSuper?locale=en-US"
        val response = getKronchMetadataInfo(tttt).data.first()
        val dubEps = ArrayList<Episode>()
        val subEps = if (isMovie) getMovie(seriesIDSuper)  else ArrayList()
        val title = response.title.toString()
        val plot = response.description.toString()
        val year = response.seriesLaunchYear
        val poster = response.images?.posterTall?.get(0)?.get(6)?.source ?: ""
        val backposter = response.images?.posterWide?.get(0)?.get(7)?.source ?: ""
        val tags = response.keywords
        val infodata = "{\"tvtype\":\"$type\",\"seriesID\":\"$seriesIDSuper\"}"
        val recommendations = getRecommendations(seriesIDSuper)
        if (!isMovie) {
            val nn = app.get("$krunchyapi/content/v2/cms/series/$seriesIDSuper/seasons?locale=en-US", headers = latestKrunchyHeader).parsed<BetaKronch>()
            val inn = nn.data.filter {
                !it.title!!.contains(Regex("Piece: East Blue|Piece: Alabasta|Piece: Sky Island"))
            }
            inn.apmap { nntwo ->
                val audioaa = nntwo.audioLocale == "ja-JP" || nntwo.audioLocale == "zh-CN"
                val versions = nntwo.versions
                val sss = nntwo.id
                if (!versions.isNullOrEmpty()) {
                    versions.filter {
                        it.audioLocale == "ja-JP" || it.audioLocale == "zh-CN" || it.audioLocale == "en-US" || it.audioLocale?.isEmpty() == true
                    }.forEach {
                        val guid = it.guid
                        val res = app.get("$krunchyapi/content/v2/cms/seasons/$guid/episodes?&locale=en-US", headers = latestKrunchyHeader).parsed<BetaKronch>()
                        res.data.filter {
                            it.isClip == false
                        }.apmap {
                            val issub = it.audioLocale == "ja-JP"  || it.audioLocale == "zh-CN" || it.audioLocale?.isEmpty() == true
                            val isdub = it.audioLocale == "en-US"
                            if (issub) {
                                subEps.add(getEpisode(it, true))
                            }
                            if (isdub)  {
                                dubEps.add(getEpisode(it, false))
                            }
                        }
                    }
                }

                if (audioaa) {
                    val res = app.get("$krunchyapi/content/v2/cms/seasons/$sss/episodes?&locale=en-US", headers = latestKrunchyHeader).parsed<BetaKronch>()
                    res.data.filter {
                        it.isClip == false
                    }.apmap { data ->
                        subEps.add(getEpisode(data, true))
                    }
                }
            }
        }
        return newAnimeLoadResponse(title, infodata, TvType.Anime) {
            if (subEps.isNotEmpty()) addEpisodes(DubStatus.Subbed,subEps.distinct().toList())
            if (dubEps.isNotEmpty()) addEpisodes(DubStatus.Dubbed,dubEps.distinct().toList())
            this.plot = plot
            this.tags = tags
            this.year = year
            this.posterUrl = poster
            this.backgroundPosterUrl = backposter
            this.recommendations = recommendations
        }

    }


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

    data class BetaKronchStreams (
        @JsonProperty("media_id"         ) var mediaId        : String?             = null,
        @JsonProperty("audio_locale"     ) var audioLocale    : String?             = null,
        @JsonProperty("subtitles"        ) var subtitles      : HashMap<String, Subtitle>?          = HashMap(),
        @JsonProperty("streams"          ) var streams        : Testt?            = Testt(),
    )


    data class Subtitle (
        val locale: String? = null,
        val url: String? = null,
        val format: String? = null,
    )

    data class Testt (
        @JsonProperty("adaptive_hls")val adaptiveHLS: Map<String, BetaKronchS>? = null,
        @JsonProperty("vo_adaptive_hls")val vrvHLS: Map<String, BetaKronchS>? = null,
    )



    data class BetaKronchS (
        @JsonProperty("hardsub_locale" ) var hardsubLocale : String? = null,
        @JsonProperty("url"            ) var url           : String? = null,
    )


    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        getKronchToken()
        val parsedata = parseJson<EpsInfo>(data)
        val consuToken = app.get("https://cronchy.consumet.stream/token").parsed<ConsuToken>()
        val mediaId = parsedata.id
        val issub = parsedata.issub == true
        val response = app.get("$krunchyapi/cms/v2${consuToken.bucket}/videos/$mediaId/streams?Policy=${consuToken.policy}&Signature=${consuToken.signature}&Key-Pair-Id=${consuToken.keyPairId}").parsed<BetaKronchStreams>()
        val aa = response.streams?.vrvHLS ?: response.streams?.adaptiveHLS ?: return false

        aa.entries.filter {
            it.key == "en-US" || it.key.isEmpty()
        }.map {
            it.value
        }.apmap {
            val raw = it.hardsubLocale?.isEmpty()
            val hardsubinfo = it.hardsubLocale?.contains("en-US")
            val vvv = if (it.url!!.contains("vrv.co")) "_VRV" else ""
            val name = if (raw == false && issub) "Kronch$vvv Hardsub English (US)" else if (raw == true && issub) "Kronch$vvv RAW" else "Kronch$vvv English"
            if (hardsubinfo == true && issub) {
                getKronchStream(it.url!!, name, callback)
            }
            if (raw == true) {
                getKronchStream(it.url!!, name, callback)
            }
        }
        response.subtitles?.map {
            it.value
        }?.map {
            val lang = when (it.locale){
                "ja-JP" -> "Japanese"
                "en-US" -> "English"
                "de-DE" -> "German"
                "es-ES" -> "Spanish"
                "es-419" -> "Spanish LAT"
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
                else -> "[${it.locale}] "
            }
            val url = it.url
            subtitleCallback.invoke(
                SubtitleFile(lang,url!!)
            )
        }

        return true
    }
}