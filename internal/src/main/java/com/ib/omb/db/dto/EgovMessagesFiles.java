package com.ib.omb.db.dto;

import static javax.persistence.GenerationType.SEQUENCE;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * EgovMessagesFiles @author n.kosev
 */
@Entity
@Table(name = "EGOV_MESSAGES_FILES")
public class EgovMessagesFiles  implements java.io.Serializable {

	private static final long serialVersionUID = -3063804870630448946L;
	
	@SequenceGenerator(name = "EgovMessagesFiles", sequenceName = "SEQ_EGOV_MESSAGES_FILES", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "EgovMessagesFiles")
	@Column(name = "ID", unique = true, nullable = false)
	private Integer id;
	
	@Column(name = "ID_MESSAGE")
	private Integer idMessage;
	
	@Column(name = "FILENAME")
	private String filename;
	
	@Column(name = "MIME")
	private String mime;
	
	@Column(name = "BLOBCONTENT")
	private byte[] 	blobcontent;
	
	@Column(name = "GUID")
	private String guid;

	public EgovMessagesFiles() {
		
	}
	
	

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getIdMessage() {
		return idMessage;
	}

	public void setIdMessage(Integer idMessage) {
		this.idMessage = idMessage;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getMime() {
		return mime;
	}

	public void setMime(String mime) {
		this.mime = mime;
	}

	public byte[] getBlobcontent() {
		return blobcontent;
	}

	public void setBlobcontent(byte[] blobcontent) {
		this.blobcontent = blobcontent;
	}

	public String getGuid() {
		return this.guid;
	}

	public void setGuid(String guid) {
		this.guid = guid;
	}
}
