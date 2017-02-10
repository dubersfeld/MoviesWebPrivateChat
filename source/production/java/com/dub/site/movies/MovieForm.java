package com.dub.site.movies;

import java.util.Date;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;

import com.dub.site.validation.NotTooOld;


public class MovieForm {
	
	
	@NotNull(message = "{validate.title.required}")
	@Size(min = 1, message = "{validate.title.required}")
	protected String title;
	
	@NotTooOld(message = "{validate.releaseDate.remote}")
	@DateTimeFormat(pattern="yyyy/MM/dd")
	@NotNull(message = "{validate.releaseDate.required}")
	protected Date releaseDate;
	
	@NotNull(message = "{validate.directorId.required}")
	@Min(value  = 1, message = "{validate.min.directorId}")
	protected Integer directorId;
	
	@NotNull(message = "{validate.runningTime.required}")
	protected Integer runningTime;
	
	
 
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	

	public Date getReleaseDate() {
		return releaseDate;
	}
	public void setReleaseDate(Date releaseDate) {
		this.releaseDate = releaseDate;
	}
		  
	
	public Integer getDirectorId() {
		return directorId;
	}
	public void setDirectorId(Integer directorId) {
		this.directorId = directorId;
	}
	
	
	public Integer getRunningTime() {
		return runningTime;
	}
	public void setRunningTime(Integer runningTime) {
		this.runningTime = runningTime;
	}
	
}// class
