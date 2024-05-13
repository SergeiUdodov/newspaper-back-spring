package com.nic.newspaper.entity;

import java.util.Date;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "article", schema = "public")
public class Article {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "header")
	private String header;

	@Column(name = "content")
	private String content;

	@Column(name = "date")
	private Date date;
	private String formattedDate;

	@Column(name = "imageURL")
	private String imageURL;

	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinTable(name = "articles_comments", joinColumns = @JoinColumn(name = "article_id"), inverseJoinColumns = @JoinColumn(name = "comment_id"))
	private List<Comment> comments;

	@ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinTable(name = "articles_likes", joinColumns = @JoinColumn(name = "article_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
	private List<User> likes;

	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinTable(name = "articles_themes", joinColumns = @JoinColumn(name = "article_id"), inverseJoinColumns = @JoinColumn(name = "theme_id"))
	private List<Theme> themes;

	public Article(String header, String content, Date date) {
		this.header = header;
		this.content = content;
		this.date = date;
	}

}
