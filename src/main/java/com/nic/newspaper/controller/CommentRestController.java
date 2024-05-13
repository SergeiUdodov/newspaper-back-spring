package com.nic.newspaper.controller;

import java.text.SimpleDateFormat;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nic.newspaper.entity.Comment;
import com.nic.newspaper.model.CrmComment;
import com.nic.newspaper.service.CommentService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class CommentRestController {

	@Autowired
	private CommentService commentService;

	@PostMapping("/addComment/{articleId}")
	public void addComment(@PathVariable long articleId, @RequestBody CrmComment theComment,
			HttpServletRequest request) {

		commentService.save(articleId, theComment, request);

	}

	@DeleteMapping("/deleteComment/{commentId}")
	public String deleteComment(@PathVariable long commentId) {

		commentService.deleteCommentById(commentId);

		return "Deleted Comment id - " + commentId;

	}

	@GetMapping("/comments/{aritcleId}")
	public List<Comment> aritcleComments(@PathVariable long aritcleId) {
		
		return commentService.articleComments(aritcleId);
	}

}
