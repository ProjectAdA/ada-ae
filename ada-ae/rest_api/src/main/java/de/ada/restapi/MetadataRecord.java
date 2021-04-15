package de.ada.restapi;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MetadataRecord {

	public MetadataRecord() {
	}

	@JsonProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")
	@JsonAlias("type")
	private String type = "http://schema.org/VideoObject";

	@JsonProperty("https://w3id.org/idsa/core/checkSum")
	@JsonAlias("id")
	private String id;
	
	@JsonProperty("http://purl.org/dc/elements/1.1/identifier")
	@JsonAlias("shortId")
	private Integer shortId;

	@JsonProperty("http://www.w3.org/2000/01/rdf-schema#label")
	@JsonAlias("title")
	private String title;

	@JsonProperty("http://dbpedia.org/ontology/year")
	@JsonAlias("year")
	private Integer year;

	@JsonProperty("http://dbpedia.org/ontology/Work/runtime")
	@JsonAlias("runtime")
	private Integer runtime;

	@JsonProperty("http://schema.org/duration")
	@JsonAlias("duration")
	private Integer duration;

	@JsonProperty("http://schema.org/genre")
	@JsonAlias("category")
	private String category;

	@JsonProperty("http://schema.org/url")
	@JsonAlias("playoutUrl")
	private String playoutUrl;

	@JsonProperty("http://schema.org/director")
	@JsonAlias("director")
	private String director;

	@JsonProperty("http://dbpedia.org/ontology/releaseDate")
	@JsonAlias("releaseDate")
	private String releaseDate;

	@JsonProperty("http://dbpedia.org/ontology/writer")
	@JsonAlias("writer")
	private String writer;

	@JsonProperty("http://purl.org/dc/terms/language")
	@JsonAlias("language")
	private String language;

	@JsonProperty("http://schema.org/broadcaster")
	@JsonAlias("broadcaster")
	private String broadcaster;

	@JsonProperty("http://dbpedia.org/ontology/imdbId")
	@JsonAlias("imdbId")
	private String imdbId;

	@JsonProperty("http://dbpedia.org/ontology/actor")
	@JsonAlias("actors")
	private String actors;

	@JsonProperty("http://dbpedia.org/ontology/abstract")
	@JsonAlias("theabstract")
	private String theabstract;

	@JsonProperty("http://dbpedia.org/ontology/filmVersion")
	@JsonAlias("filmVersion")
	private String filmVersion;

	public String getType() {
		return type;
	}

	public void setType(String newType) {
		this.type = newType == null ? newType : newType.trim();
	}

	public String getId() {
		return id;
	}

	public void setId(String newId) {
		this.id = newId == null ? newId : newId.trim();
	}

	public Integer getShortId() {
		return shortId;
	}

	public void setShortId(Integer newShortId) {
		this.shortId = newShortId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String newTitle) {
		this.title = newTitle == null ? newTitle : newTitle.trim();
	}

	public Integer getYear() {
		return year;
	}

	public void setYear(Integer newYear) {
		this.year = newYear;
	}

	public Integer getRuntime() {
		return runtime;
	}

	public void setRuntime(Integer newRuntime) {
		this.runtime = newRuntime;
	}

	public Integer getDuration() {
		return duration;
	}

	public void setDuration(Integer newDuration) {
		this.duration = newDuration;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String newCategory) {
		this.category = newCategory == null ? newCategory : newCategory.trim();
	}

	public String getPlayoutUrl() {
		return playoutUrl;
	}

	public void setPlayoutUrl(String newPlayoutUrl) {
		this.playoutUrl = newPlayoutUrl == null ? newPlayoutUrl : newPlayoutUrl.trim();
	}

	public String getDirector() {
		return director;
	}

	public void setDirector(String newDirector) {
		this.director = newDirector == null ? newDirector : newDirector.trim();
	}

	public String getReleaseDate() {
		return releaseDate;
	}

	public void setReleaseDate(String newReleaseDate) {
		this.releaseDate = newReleaseDate == null ? newReleaseDate : newReleaseDate.trim();
	}

	public String getWriter() {
		return writer;
	}

	public void setWriter(String newWriter) {
		this.writer = newWriter == null ? newWriter : newWriter.trim();
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String newLanguage) {
		this.language = newLanguage == null ? newLanguage : newLanguage.trim();
	}

	public String getBroadcaster() {
		return broadcaster;
	}

	public void setBroadcaster(String newBroadcaster) {
		this.broadcaster = newBroadcaster == null ? newBroadcaster : newBroadcaster.trim();
	}

	public String getImdbId() {
		return imdbId;
	}

	public void setImdbId(String newImdbId) {
		this.imdbId = newImdbId == null ? newImdbId : newImdbId.trim();
	}

	public String getActors() {
		return actors;
	}

	public void setActors(String newActors) {
		this.actors = newActors == null ? newActors : newActors.trim();
	}

	public String getTheabstract() {
		return theabstract;
	}

	public void setTheabstract(String newTheabstract) {
		this.theabstract = newTheabstract == null ? newTheabstract : newTheabstract.trim();
	}

	public String getFilmVersion() {
		return filmVersion;
	}

	public void setFilmVersion(String newFilmVersion) {
		this.filmVersion = newFilmVersion == null ? newFilmVersion : newFilmVersion.trim();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((actors == null) ? 0 : actors.hashCode());
		result = prime * result + ((broadcaster == null) ? 0 : broadcaster.hashCode());
		result = prime * result + ((category == null) ? 0 : category.hashCode());
		result = prime * result + ((director == null) ? 0 : director.hashCode());
		result = prime * result + ((duration == null) ? 0 : duration.hashCode());
		result = prime * result + ((filmVersion == null) ? 0 : filmVersion.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((imdbId == null) ? 0 : imdbId.hashCode());
		result = prime * result + ((language == null) ? 0 : language.hashCode());
		result = prime * result + ((playoutUrl == null) ? 0 : playoutUrl.hashCode());
		result = prime * result + ((releaseDate == null) ? 0 : releaseDate.hashCode());
		result = prime * result + ((runtime == null) ? 0 : runtime.hashCode());
		result = prime * result + ((shortId == null) ? 0 : shortId.hashCode());
		result = prime * result + ((theabstract == null) ? 0 : theabstract.hashCode());
		result = prime * result + ((title == null) ? 0 : title.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((writer == null) ? 0 : writer.hashCode());
		result = prime * result + ((year == null) ? 0 : year.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MetadataRecord other = (MetadataRecord) obj;
		if (actors == null) {
			if (other.actors != null)
				return false;
		} else if (!actors.equals(other.actors))
			return false;
		if (broadcaster == null) {
			if (other.broadcaster != null)
				return false;
		} else if (!broadcaster.equals(other.broadcaster))
			return false;
		if (category == null) {
			if (other.category != null)
				return false;
		} else if (!category.equals(other.category))
			return false;
		if (director == null) {
			if (other.director != null)
				return false;
		} else if (!director.equals(other.director))
			return false;
		if (duration == null) {
			if (other.duration != null)
				return false;
		} else if (!duration.equals(other.duration))
			return false;
		if (filmVersion == null) {
			if (other.filmVersion != null)
				return false;
		} else if (!filmVersion.equals(other.filmVersion))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (imdbId == null) {
			if (other.imdbId != null)
				return false;
		} else if (!imdbId.equals(other.imdbId))
			return false;
		if (language == null) {
			if (other.language != null)
				return false;
		} else if (!language.equals(other.language))
			return false;
		if (playoutUrl == null) {
			if (other.playoutUrl != null)
				return false;
		} else if (!playoutUrl.equals(other.playoutUrl))
			return false;
		if (releaseDate == null) {
			if (other.releaseDate != null)
				return false;
		} else if (!releaseDate.equals(other.releaseDate))
			return false;
		if (runtime == null) {
			if (other.runtime != null)
				return false;
		} else if (!runtime.equals(other.runtime))
			return false;
		if (shortId == null) {
			if (other.shortId != null)
				return false;
		} else if (!shortId.equals(other.shortId))
			return false;
		if (theabstract == null) {
			if (other.theabstract != null)
				return false;
		} else if (!theabstract.equals(other.theabstract))
			return false;
		if (title == null) {
			if (other.title != null)
				return false;
		} else if (!title.equals(other.title))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		if (writer == null) {
			if (other.writer != null)
				return false;
		} else if (!writer.equals(other.writer))
			return false;
		if (year == null) {
			if (other.year != null)
				return false;
		} else if (!year.equals(other.year))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "MetadataRecord [type=" + type + ", id=" + id + ", shortId=" + shortId + ", title="
				+ title + ", year=" + year + ", runtime=" + runtime + ", duration=" + duration
				+ ", category=" + category + ", playoutUrl=" + playoutUrl + ", director=" + director
				+ ", releaseDate=" + releaseDate + ", writer=" + writer + ", language=" + language
				+ ", broadcaster=" + broadcaster + ", imdbId=" + imdbId + ", actors=" + actors
				+ ", theabstract=" + theabstract + ", filmVersion=" + filmVersion + "]";
	}

}
