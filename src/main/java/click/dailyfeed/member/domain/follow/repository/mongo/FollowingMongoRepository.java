package click.dailyfeed.member.domain.follow.repository.mongo;

import click.dailyfeed.member.domain.follow.document.FollowingDocument;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface FollowingMongoRepository extends MongoRepository<FollowingDocument, ObjectId> {
    Optional<FollowingDocument> findByFromIdAndToId(Long fromId, Long toId);
}
