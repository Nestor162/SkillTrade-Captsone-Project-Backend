package nestorcicardini.skilltrade.posts;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import nestorcicardini.skilltrade.interests.Interest;
import nestorcicardini.skilltrade.posts.Post.Availability;
import nestorcicardini.skilltrade.posts.Post.PostStatus;
import nestorcicardini.skilltrade.posts.Post.SkillLevel;
import nestorcicardini.skilltrade.posts.payloads.ChangeStatusPayload;
import nestorcicardini.skilltrade.posts.payloads.PublishPostPayload;
import nestorcicardini.skilltrade.users.UserUtils;

@RestController
@RequestMapping("/posts")
@PreAuthorize("hasAuthority('USER') or hasAuthority('ADMIN')")
public class PostController {
	@Autowired
	PostService postService;

	@Autowired
	// Custom class that have useful methods to check roles/authorities and to
	// get info from authenticated user
	UserUtils userUtils;

	@Autowired
	PostRepository postRepo;

	// CRUD:
	// 1. CREATE (POST METHOD) - http://localhost:3001/posts
	@PostMapping("")
	public ResponseEntity<Post> publish(
			@RequestBody @Validated PublishPostPayload body) {

		Post newPost = postService.publish(body);
		return new ResponseEntity<>(newPost, HttpStatus.CREATED);
	}

//	// 2. READ (GET METHOD) - http://localhost:3001/posts
	@GetMapping("")
	@PreAuthorize("hasAnyAuthority('ADMIN', 'USER')")
	public Page<Post> getAllPosts(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "id") String sortValue) {

		return postService.findWithPagination(page, size, sortValue);
	}

	// 4. READ (GET METHOD) - http://localhost:3001/posts/{postId}
	@GetMapping("/{postId}")
	@PreAuthorize("hasAnyAuthority('ADMIN', 'USER')")
	public Post getPostById(@PathVariable String postId) {
		return postService.getPostById(postId);
	}

	// 4.5 READ (GET METHOD) - http://localhost:3001/posts?author={postId}
	@GetMapping(params = "author")
	@PreAuthorize("hasAnyAuthority('ADMIN', 'USER')")
	public List<Post> getPostByAuthorId(@RequestParam String author) {
		return postService.getPostsByProfileId(author);
	}

	// 5. UPDATE (PUT METHOD) - http://localhost:3001/post/:postId + req. body
	@PutMapping("/{postId}")
	@PreAuthorize("hasAuthority('ADMIN') or @postService.getPostById(#postId).getProfile().getUser().getId().toString() == @userUtils.getCurrentUserId().toString()")
	public Post updateUser(@PathVariable String postId,
			@RequestBody @Validated PublishPostPayload body) throws Exception {

		return postService.findByIdAndUpdate(postId, body);
	}

	// 6. DELETE (DELETE METHOD) - http://localhost:3001/posts/:postId
	@DeleteMapping("/{postId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasAuthority('ADMIN') or @postService.getPostById(#postId).getProfile().getUser().getId().toString() == @userUtils.getCurrentUserId().toString()")
	public void deleteUser(@PathVariable String postId) {
		postService.findByIdAndDelete(postId);
	}

	// 7. UPDATE STATUS (PATCH METHOD) - http://localhost:3001/posts/:postId +
	// req. body
	@PatchMapping("/{postId}")
	public Post changePostStatus(@PathVariable String postId,
			@RequestBody @Validated ChangeStatusPayload Status)
			throws Exception {
		return postService.findByIdAndChangeStatus(postId, Status);
	}

	// FILTERS
	@GetMapping("/filters")
	public Page<Post> getPosts(
			@RequestParam(required = false) Availability availability,
			@RequestParam(required = false) Interest category,
			@RequestParam(required = false) SkillLevel skillLevel,
			@RequestParam(required = false) PostStatus status,
			@RequestParam(required = false) String title,
			@RequestParam(required = false) String query,
			@RequestParam(required = false) String sort,
			@RequestParam(required = false) String location,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {

		if (size < 0)
			size = 10;
		if (size > 100)
			size = 100;

		// Create a specification to filter posts
		Specification<Post> spec = Specification.where(null);
		if (availability != null) {
			spec = spec
					.and(PostSpecifications.availabilityEquals(availability));
		}
		if (category != null) {
			spec = spec.and(PostSpecifications.categoryEquals(category));
		}
		if (skillLevel != null) {
			spec = spec.and(PostSpecifications.skillLevelEquals(skillLevel));
		}
		if (status != null) {
			spec = spec.and(PostSpecifications.statusEquals(status));
		}
		if (title != null) {
			spec = spec.and(PostSpecifications.titleContains(title));
		}
		if (query != null) {
			spec = spec
					.and(PostSpecifications.titleOrDescriptionContains(query));
		}
		if (location != null) {
			spec = spec.and(PostSpecifications.authorLocationEquals(location));
		}

		// Create a Sort object based on the sort parameter
		Sort sortObj;
		if ("newest".equals(sort)) {
			sortObj = Sort.by(Sort.Direction.DESC, "publicationDate");
		} else if ("oldest".equals(sort)) {
			sortObj = Sort.by(Sort.Direction.ASC, "publicationDate");
		} else if ("alphabetical".equals(sort)) {
			sortObj = Sort.by(Sort.Direction.ASC, "title");
		} else if ("reverseAlphabetical".equals(sort)) {
			sortObj = Sort.by(Sort.Direction.DESC, "title");
		} else {
			sortObj = Sort.unsorted();
		}

		// Create a Pageable object
		Pageable pageable = PageRequest.of(page, size, sortObj);

		// Find a page of posts matching the specification
		return postRepo.findAll(spec, pageable);
	}

}
