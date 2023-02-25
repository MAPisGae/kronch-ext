// use an integer for version numbers
version = 13


cloudstream {
    language = "es"
    // All of these properties are optional, you can safely remove them

    description = "Observa el anime de ese sitio con la ayuda de la API Consumet, este proveedor solo mostrará anime subtitulado/doblado en español, subtítulos de varios idiomas disponibles, el catalogo disponible es de los Estados Unidos"
    authors = listOf("Stormunblessed")

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
     * */
    status = 0 // will be 3 if unspecified
    tvTypes = listOf(
        "Anime",
        "OVA",
    )

    iconUrl = "https://avatars.githubusercontent.com/u/105397082?s=300&v=4"
}
