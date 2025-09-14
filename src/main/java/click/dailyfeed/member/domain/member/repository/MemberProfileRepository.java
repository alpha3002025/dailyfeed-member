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
    @Query("SELECT mp FROM MemberProfile mp JOIN FETCH mp.member m WHERE m.id = :memberId")
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


    @Query(value = "SELECT DISTINCT mp FROM MemberProfile mp " +
            "INNER JOIN mp.member m " +
            "LEFT JOIN FETCH mp.profileImages img " +
            "WHERE m.id IN :memberIds " +
            "AND mp.isActive = true " +
            "AND (img.isPrimary = true OR img IS NULL)")
    List<MemberProfile> findWithImagesByMemberIdsIn(@Param("memberIds") List<Long> memberIds);

}
