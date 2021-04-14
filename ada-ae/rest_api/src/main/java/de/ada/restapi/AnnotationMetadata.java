package de.ada.restapi;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AnnotationMetadata {
	public AnnotationMetadata(String annoUri, Interval interval) {
		super();
		this.annoUri = annoUri;
		this.interval = interval;
		this.matches = new HashMap<String, Set<AnnotationMetadata>>();
	}
	public String annoUri;
	public Interval interval;
	public Map<String, Set<AnnotationMetadata>> matches;
	public String searchValue;
	
	public String getMediaId() {
		String result = null;
		String ids = this.annoUri.replace(URIconstants.MEDIA_PREFIX(), "");
		String[] split = ids.split("/");
		if (split.length == 2) {
			result = split[0];
		}
		return result;
	}

	public String getAnnotationId() {
		String result = null;
		String ids = this.annoUri.replace(URIconstants.MEDIA_PREFIX(), "");
		String[] split = ids.split("/");
		if (split.length == 2) {
			result = split[1];
		}
		return result;
	}

	public String getAnnoUri() {
		return annoUri;
	}
	public void setAnnoUri(String annoUri) {
		this.annoUri = annoUri;
	}
	public Interval getInterval() {
		return interval;
	}
	public void setInterval(Interval interval) {
		this.interval = interval;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((annoUri == null) ? 0 : annoUri.hashCode());
		result = prime * result + ((interval == null) ? 0 : interval.hashCode());
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
		AnnotationMetadata other = (AnnotationMetadata) obj;
		if (annoUri == null) {
			if (other.annoUri != null)
				return false;
		} else if (!annoUri.equals(other.annoUri))
			return false;
		if (interval == null) {
			if (other.interval != null)
				return false;
		} else if (!interval.equals(other.interval))
			return false;
		return true;
	}

	@Override
	public String toString() {
		String result = "AnnotationMetadata [annoUri=" + annoUri + ", interval=" + interval
				 + ", matches=";
		
		Set<String> collect = matches.keySet().stream().flatMap(k -> matches.get(k).stream().map(a -> a.getAnnoUri())).collect(Collectors.toSet());
		
		result = result + collect.toString() + "]";
		return result;
	}

}
