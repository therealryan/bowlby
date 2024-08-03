package dev.flowty.bowlby.app.github;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * API message definitions
 */
@SuppressWarnings("javadoc")
class Message {

  private Message() {
    // no instances
  }

  static class JsonBodyHandler<T> implements HttpResponse.BodyHandler<T> {

    private Class<T> type;

    JsonBodyHandler( Class<T> type ) {
      this.type = type;
    }

    @Override
    public BodySubscriber<T> apply( HttpResponse.ResponseInfo responseInfo ) {
      return HttpResponse.BodySubscribers.mapping(
          BodySubscribers.ofString( StandardCharsets.UTF_8 ),
          body -> {
            try {
              return new ObjectMapper().readValue( body, type );
            }
            catch( IOException e ) {
              throw new UncheckedIOException( e );
            }
          } );
    }
  }

  static class ListWorkflowRunResponse {
    static JsonBodyHandler<ListWorkflowRunResponse> HANDLER = new JsonBodyHandler<>(
        ListWorkflowRunResponse.class );

    @JsonProperty("workflow_runs")
    public final List<WorkflowRun> runs;

    public ListWorkflowRunResponse(
        @JsonProperty("workflow_runs") List<WorkflowRun> runs ) {
      this.runs = runs;
    }
  }

  static class WorkflowRun {
    @JsonProperty("id")
    public final String id;
    @JsonProperty("status")
    public final String status;
    @JsonProperty("conclusion")
    public final String conclusion;

    public WorkflowRun(
        @JsonProperty("id") String id,
        @JsonProperty("status") String status,
        @JsonProperty("conclusion") String conclusion ) {
      this.id = id;
      this.status = status;
      this.conclusion = conclusion;
    }
  }
}
