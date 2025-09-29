package click.dailyfeed.member.domain.follow.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "followings")
public class FollowingDocument {
    @Id
    private ObjectId id;

    @Field
    private Long fromId;

    @Field
    private Long toId;

    @Builder(builderMethodName = "newFollowingBuilder", builderClassName = "NewFollowing")
    public FollowingDocument(Long fromId, Long toId) {
        this.fromId = fromId;
        this.toId = toId;
    }
}
