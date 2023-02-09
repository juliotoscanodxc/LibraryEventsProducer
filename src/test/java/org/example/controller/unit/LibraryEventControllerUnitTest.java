package org.example.controller.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.controller.LibraryEventsController;
import org.example.model.Book;
import org.example.model.LibraryEvent;
import org.example.producer.LibraryEventProducer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(LibraryEventsController.class)
@AutoConfigureMockMvc
public class LibraryEventControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LibraryEventProducer producer;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void postLibraryEvent() throws Exception {
        Book book = Book.builder()
                .id(123)
                .author("Julio")
                .name("Kafka using Spring Boot")
                .build();

        LibraryEvent libraryEvent = LibraryEvent.builder()
                .id(null)
                .book(book)
                .build();

         String json = objectMapper.writeValueAsString(libraryEvent);
         when(producer.sendLibraryEventApproachTwo(isA(LibraryEvent.class))).thenReturn(null);

         mockMvc.perform(post("/v1/libraryevent")
                 .content(json)
                 .contentType(MediaType.APPLICATION_JSON))
                 .andExpect(status().isCreated());
    }

    @Test
    void postLibraryEvent_4xx() throws Exception {

        Book book = Book.builder()
                .id(null)
                .author(null)
                .name("Kafka using Spring Boot")
                .build();

        LibraryEvent libraryEvent = LibraryEvent.builder()
                .id(null)
                .book(book)
                .build();

        String json = objectMapper.writeValueAsString(libraryEvent);
        when(producer.sendLibraryEventApproachTwo(isA(LibraryEvent.class))).thenReturn(null);
        String expectedErrorMessage = "book.bookAuthor - must not be blank, book.bookId - must not be null";
        mockMvc.perform(post("/v1/libraryevent")
                        .content(json)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string(expectedErrorMessage));

    }

    @Test
    void updateLibraryEvent() throws Exception {

        Book book = Book.builder()
                .id(123)
                .author("Julio")
                .name("Kafka using Spring Boot")
                .build();

        LibraryEvent libraryEvent = LibraryEvent.builder()
                .id(123)
                .book(book)
                .build();
        String json = objectMapper.writeValueAsString(libraryEvent);
        when(producer.sendLibraryEventApproachTwo(isA(LibraryEvent.class))).thenReturn(null);

        mockMvc.perform(
                        put("/v1/libraryevent")
                                .content(json)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

    }

    @Test
    void updateLibraryEvent_withNullLibraryEventId() throws Exception {

        Book book = Book.builder()
                .id(123)
                .author("Julio")
                .name("Kafka using Spring Boot")
                .build();

        LibraryEvent libraryEvent = LibraryEvent.builder()
                .id(null)
                .book(book)
                .build();
        String json = objectMapper.writeValueAsString(libraryEvent);
        when(producer.sendLibraryEventApproachTwo(isA(LibraryEvent.class))).thenReturn(null);

        mockMvc.perform(
                        put("/v1/libraryevent")
                                .content(json)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string("Please pass the LibraryEventId"));

    }
}
