/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.db.user;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class GroupMembershipDao implements Dao {

  public List<GroupMembershipDto> selectGroups(SqlSession session, GroupMembershipQuery query, Long userId, int offset, int limit) {
    Map<String, Object> params = ImmutableMap.of("query", query, "userId", userId);
    return mapper(session).selectGroups(params, new RowBounds(offset, limit));
  }

  public int countGroups(SqlSession session, GroupMembershipQuery query, Long userId) {
    Map<String, Object> params = ImmutableMap.of("query", query, "userId", userId);
    return mapper(session).countGroups(params);
  }

  public List<UserMembershipDto> selectMembers(SqlSession session, UserMembershipQuery query, int offset, int limit) {
    Map<String, Object> params = ImmutableMap.of("query", query, "groupId", query.groupId());
    return mapper(session).selectMembers(params, new RowBounds(offset, limit));
  }

  public int countMembers(SqlSession session, UserMembershipQuery query) {
    Map<String, Object> params = ImmutableMap.of("query", query, "groupId", query.groupId());
    return mapper(session).countMembers(params);
  }

  public Map<String, Integer> countUsersByGroups(DbSession session, Collection<Long> groupIds) {
    Map<String, Integer> result = Maps.newHashMap();
    executeLargeInputs(
      groupIds,
      input -> {
        List<GroupUserCount> userCounts = mapper(session).countUsersByGroup(input);
        for (GroupUserCount count : userCounts) {
          result.put(count.groupName(), count.userCount());
        }
        return userCounts;
      });

    return result;
  }

  public Multimap<String, String> selectGroupsByLogins(DbSession session, Collection<String> logins) {
    Multimap<String, String> result = ArrayListMultimap.create();
    executeLargeInputs(
      logins,
      input -> {
        List<LoginGroup> groupMemberships = mapper(session).selectGroupsByLogins(input);
        for (LoginGroup membership : groupMemberships) {
          result.put(membership.login(), membership.groupName());
        }
        return groupMemberships;
      });

    return result;
  }

  private static GroupMembershipMapper mapper(SqlSession session) {
    return session.getMapper(GroupMembershipMapper.class);
  }
}
