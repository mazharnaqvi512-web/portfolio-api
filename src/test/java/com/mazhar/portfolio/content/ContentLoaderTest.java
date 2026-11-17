package com.mazhar.portfolio.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import com.mazhar.portfolio.dto.ProjectDto;

class ContentLoaderTest {

    private final ContentLoader loader = new ContentLoader();

    @Test
    void refusesAPropertyTheDtoDoesNotDeclareInsteadOfDroppingItSilently() {
        assertThatThrownBy(() -> loader.loadList("unknown-field.json", ProjectDto.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("content/unknown-field.json")
            .hasMessageContaining("aFieldTheDtoDoesNotDeclare");
    }

    @Test
    void namesTheFileWhenItIsMissing() {
        assertThatThrownBy(() -> loader.loadList("does-not-exist.json", ProjectDto.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("content/does-not-exist.json");
    }

    @Test
    void loadedListsCannotBeModified() {
        assertThatThrownBy(() -> loader.loadList("projects.json", ProjectDto.class).clear())
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void readsTheSameFileTwiceToTheSameContent() {
        assertThat(loader.loadList("projects.json", ProjectDto.class))
            .isEqualTo(loader.loadList("projects.json", ProjectDto.class));
    }
}
