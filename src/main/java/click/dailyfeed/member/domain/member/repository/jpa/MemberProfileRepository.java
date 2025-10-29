package click.dailyfeed.member.domain.member.repository.jpa;

import click.dailyfeed.member.domain.member.entity.MemberProfile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberProfileRepository extends JpaRepository<MemberProfile, Long> {

    @Query("SELECT mp FROM MemberProfile mp JOIN FETCH mp.member m LEFT JOIN FETCH mp.profileImages mpi WHERE m.id = :memberId")
    Optional<MemberProfile> findMemberProfileByMemberId(@Param("memberId") Long memberId);

    @Query("SELECT mp FROM MemberProfile mp JOIN FETCH mp.member m LEFT JOIN FETCH mp.profileImages mpi WHERE mp.handle = :handle")
    Optional<MemberProfile> findByHandle(String handle);

    @Query(value = "SELECT DISTINCT mp FROM MemberProfile mp " +
            "INNER JOIN mp.member m " +
            "LEFT JOIN FETCH mp.profileImages img " +
            "WHERE m.id IN :memberIds " +
            "AND mp.isActive = true " +
            "AND (img.isPrimary = true OR img IS NULL)")
    Slice<MemberProfile> findWithImagesByMemberIdsIn(@Param("memberIds") List<Long> memberIds, Pageable pageable);

    @Query(value = "SELECT DISTINCT mp FROM MemberProfile mp " +
            "INNER JOIN mp.member m " +
            "LEFT JOIN FETCH mp.profileImages img " +
            "WHERE mp.isActive = true " +
            "AND m.id != :myId " +
            "AND NOT EXISTS (SELECT 1 FROM Follow f WHERE f.follower.id = :myId AND f.following.id = m.id) " +
            "AND (img.isPrimary = true OR img IS NULL) " +
            "ORDER BY m.createdAt DESC")
    Slice<MemberProfile> findWithImagesOrderByCreatedAtWithPaging(Long myId, Pageable pageable);


    @Query(value = "SELECT DISTINCT mp FROM MemberProfile mp " +
            "INNER JOIN mp.member m " +
            "LEFT JOIN FETCH mp.profileImages img " +
            "WHERE m.id IN :memberIds " +
            "AND mp.isActive = true " +
            "AND (img.isPrimary = true OR img IS NULL)")
    List<MemberProfile> findWithImagesByMemberIdsIn(@Param("memberIds") List<Long> memberIds);
}
