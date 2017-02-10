package com.dub.site.actors;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;


import org.springframework.web.multipart.MultipartFile;


/* Command object */
public class ActorPhotoMultiForm {
	
	@Min(value  = 1, message = "{validate.min.actorId}")
	@NotNull(message = "{validate.required.actorId}")
	private Integer actorId;
	
	private MultipartFile uploadedFile;

	public Integer getActorId() {
		return actorId;
	}

	public void setActorId(Integer actorId) {
		this.actorId = actorId;
	}

	public MultipartFile getUploadedFile() {
		return uploadedFile;
	}

	public void setUploadedFile(MultipartFile uploadedFile) {
		this.uploadedFile = uploadedFile;
	}

}
