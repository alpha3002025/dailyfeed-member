package click.dailyfeed.member.domain.member.repository;

import click.dailyfeed.member.domain.member.entity.MemberProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberProfileRepository extends JpaRepository<MemberProfile, Long> {

    @Query("SELECT mp FROM MemberProfile mp JOIN FETCH mp.member m LEFT JOIN FETCH mp.profileImages mpi WHERE m.id = :memberId")
    Optional<MemberProfile> findMemberProfileByMemberId(@Param("memberId") Long memberId);

    @Query(value = "SELECT DISTINCT mp FROM MemberProfile mp " +
            "INNER JOIN mp.member m " +
            "LEFT JOIN FETCH mp.profileImages img " +
            "WHERE m.id IN :memberIds " +
            "AND mp.isActive = true " +
            "AND (img.isPrimary = true OR img IS NULL)",
            countQuery = "SELECT COUNT(DISTINCT mp) FROM MemberProfile mp " +
                    "INNER JOIN mp.member m " +
                    "WHERE m.id IN :memberIds " +
                    "AND mp.isActive = true")
    Page<MemberProfile> findWithImagesByMemberIdsIn(@Param("memberIds") List<Long> memberIds, Pageable pageable);

    // TODO (아래 코드는 Frontend 테스트를 위한 임시 버전)
    /// Paging 제거 버전으로 변경할것 + 회원수 카운트는 캐시 및 단순 limit, offset 활용 (전체 멤버 수를 카운트할 경우 부하 발생)
    @Query(value = "SELECT DISTINCT mp FROM MemberProfile mp " +
            "INNER JOIN mp.member m " +
            "LEFT JOIN FETCH mp.profileImages img " +
            "WHERE mp.isActive = true " +
            "AND m.id != :myId " +
            "AND NOT EXISTS (SELECT 1 FROM Follow f WHERE f.follower.id = :myId AND f.following.id = m.id) " +
            "AND (img.isPrimary = true OR img IS NULL)",
            countQuery = "SELECT COUNT(DISTINCT mp) FROM MemberProfile mp " +
                    "INNER JOIN mp.member m " +
                    "WHERE mp.isActive = true " +
                    "AND m.id != :myId " +
                    "AND NOT EXISTS (SELECT 1 FROM Follow f WHERE f.follower.id = :myId AND f.following.id = m.id)")
    Page<MemberProfile> findWithImagesOrderByCreatedAtWithPaging(Pageable pageable, Long myId);


    @Query(value = "SELECT DISTINCT mp FROM MemberProfile mp " +
            "INNER JOIN mp.member m " +
            "LEFT JOIN FETCH mp.profileImages img " +
            "WHERE m.id IN :memberIds " +
            "AND mp.isActive = true " +
            "AND (img.isPrimary = true OR img IS NULL)")
    List<MemberProfile> findWithImagesByMemberIdsIn(@Param("memberIds") List<Long> memberIds);


    Optional<MemberProfile> findByHandle(String handle);
}
