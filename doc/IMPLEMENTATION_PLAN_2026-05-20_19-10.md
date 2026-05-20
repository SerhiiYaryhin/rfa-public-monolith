# Implementation Plan - Restore Podcast Episode Deletion
**Created: 2026-05-20 19:10**

Restore the ability to delete podcast episodes by resolving a Lombok circular `equals`/`hashCode` reference trap and refactoring the delete logic to be clean and transaction-safe.

## User Review Required

> [!IMPORTANT]
> The primary cause of the blank screen and silent failure is a **`StackOverflowError`** in the Lombok-generated `equals()` and `hashCode()` methods. Because the `PodcastChannel`, `PodcastItem`, and `PodcastItunesCategory` entities reference each other in a bidirectional manner, operations like `List.remove(episode)` trigger infinite recursion during equality checks. Excluding these bidirectional fields from equals/hashCode resolves the issue safely without impacting persistence.

## Open Questions

None at this time. The fix is straightforward and resolves the silent crash cleanly.

---

## Proposed Changes

We will apply changes to the Domain Model layer first to fix Lombok equals/hashCode generation, and then refactor the Service and Controller layers to implement clean, transactional deletion.

### Domain Model Layer

#### [MODIFY] [PodcastChannel.java](file:///home/coder/rfa/src/main/java/media/toloka/rfa/podcast/model/PodcastChannel.java)
- Exclude the `item` list and `itunescategory` list from Lombok `equals` and `hashCode` generation.
- Add Lombok `@EqualsAndHashCode.Exclude` to these fields.

#### [MODIFY] [PodcastItem.java](file:///home/coder/rfa/src/main/java/media/toloka/rfa/podcast/model/PodcastItem.java)
- Exclude the parent `chanel` reference from Lombok `equals` and `hashCode` generation.
- Add Lombok `@EqualsAndHashCode.Exclude` to `chanel`.

#### [MODIFY] [PodcastItunesCategory.java](file:///home/coder/rfa/src/main/java/media/toloka/rfa/podcast/model/PodcastItunesCategory.java)
- Exclude the parent `chanel` reference from Lombok `equals` and `hashCode` generation.
- Add Lombok `@EqualsAndHashCode.Exclude` to `chanel`.

---

### Service Layer

#### [MODIFY] [PodcastService.java](file:///home/coder/rfa/src/main/java/media/toloka/rfa/podcast/service/PodcastService.java)
- Import `org.springframework.transaction.annotation.Transactional`.
- Annotate the `DeleteEpisode` method with `@Transactional`.
- Refactor `DeleteEpisode` to safely manage the bidirectional dissociation, save the updated channel, and delete the episode entity:
  ```java
  @Transactional
  public void DeleteEpisode(PodcastItem episode, boolean deleteFile) {
      if (deleteFile && episode.getEnclosurestore() != null) {
          // Видаляємо фізичний файл та запис у Store
          storeService.DeleteInStore(episode.getEnclosurestore());
      }
      
      PodcastChannel channel = episode.getChanel();
      if (channel != null) {
          channel.getItem().remove(episode);
          episode.setChanel(null);
          chanelRepository.save(channel);
      }
      
      episodeRepository.delete(episode);
  }
  ```

---

### Controller Layer

#### [MODIFY] [EpisodeEditController.java](file:///home/coder/rfa/src/main/java/media/toloka/rfa/podcast/EpisodeEditController.java)
- Simplify `deleteEpisode` by delegating the entire deletion sequence to the transaction-wrapped `podcastService.DeleteEpisode`.
- This removes split saving/deletion logic from the controller, preventing transaction boundary issues.

---

## Verification Plan

### Automated Tests
- Run `gradlew compileJava` to ensure successful compilation.
- We will write a unit/integration test to verify that `DeleteEpisode` deletes the episode successfully without throwing `StackOverflowError` or any other JPA/Hibernate exception.

### Manual Verification
- Ask the user to verify by navigating to `/podcast/episodeedit/{puuid}/{euuid}` and clicking **"Видалити епізод"** to confirm that the deletion executes successfully and redirects to the podcast details page (`/podcast/pedit/{puuid}`).
