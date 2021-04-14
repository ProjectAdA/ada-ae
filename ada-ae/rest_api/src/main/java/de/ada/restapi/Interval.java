package de.ada.restapi;

import java.time.Instant;

public class Interval {
    Instant begin;
    Instant end;

    public Interval(long begin, long end) {
        this.begin = Instant.ofEpochMilli(begin);
        this.end = Instant.ofEpochMilli(end);
    }

	@Override
	public String toString() {
		return "Interval [begin=" + begin + ", end=" + end + "]";
	}
	
	public static boolean isThereOverlap(Interval t1, Interval t2) {
		
	    return (t1.begin.isAfter(t2.begin) || t1.begin.equals(t2.begin)) && t1.begin.isBefore(t2.end) ||
	            t1.end.isAfter(t2.begin) && (t1.end.isBefore(t2.end) || t1.end.equals(t2.end) ) ||
	            (t1.begin.isBefore(t2.begin)||t1.begin.equals(t2.begin)) && (t1.end.isAfter(t2.end)||t1.end.equals(t2.end));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((begin == null) ? 0 : begin.hashCode());
		result = prime * result + ((end == null) ? 0 : end.hashCode());
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
		Interval other = (Interval) obj;
		if (begin == null) {
			if (other.begin != null)
				return false;
		} else if (!begin.equals(other.begin))
			return false;
		if (end == null) {
			if (other.end != null)
				return false;
		} else if (!end.equals(other.end))
			return false;
		return true;
	}

}

