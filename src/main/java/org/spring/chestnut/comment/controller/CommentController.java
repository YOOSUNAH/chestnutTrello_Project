package org.spring.chestnut.comment.controller;

import lombok.RequiredArgsConstructor;
import org.spring.chestnut.comment.dto.CommentRequest;
import org.spring.chestnut.comment.dto.CommentResponse;
import org.spring.chestnut.comment.service.CommentService;
import org.spring.chestnut.global.dto.ResponseDto;
import org.spring.chestnut.global.security.UserDetailsImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/cards/{cardId}/comments")
    public ResponseEntity<ResponseDto<CommentResponse>> createComment(
        @PathVariable Long cardId,
        @RequestBody CommentRequest request,
        @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {

        CommentResponse response = commentService.createComment(cardId, request, userDetails);

        return ResponseDto.ok("댓글 작성 성공", response);
    }

}
