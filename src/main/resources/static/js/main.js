$(document).ready(function () {
  var pathname = $(location).attr("pathname");

  if (pathname != "/terminal") {
    return;
  }

  $(".terminal-out").DataTable();

  var webSocket = new WebSocket("/websocket/client", []);
  var sequenceNumber = 0;

  webSocket.addEventListener("message", (event) => {
    ++sequenceNumber;
    const response = JSON.parse(event.data);

    $.each(response.errors, function (index, error) {
      var id = "error-" + sequenceNumber + "-" + index;

      $(".terminal-output").append(
        '<article id="' +
          id +
          '" class="message is-danger"><div class="message-header"><p>Error Code : ' +
          error.code +
          '</p></div><div class="message-body">' +
          error.message +
          "</div></article>"
      );

      $("html,body").animate(
        { scrollTop: document.body.scrollHeight },
        "smooth"
      );
    });
    $.each(response.resultSets, function (index, resultSet) {
      const columns = [];
      $.each(resultSet.columns, function (index, column) {
        columns.push({
          title: column,
        });
      });
      var id = "resultset-" + sequenceNumber + "-" + index;

      $(".terminal-output").append(
        '<table id="' + id + '" class="table is-striped">'
      );

      $("#" + id).DataTable({
        columns: columns,
        data: resultSet.rows,
      });

      $("html,body").animate(
        { scrollTop: document.body.scrollHeight },
        "smooth"
      );
    });
  });

  webSocket.addEventListener("error", (event) => {
    window.location.href = "/login";
  });

  function sendMessage() {
    var sql = $("#terminal-input").val();

    $(".terminal-output").append(
      '<article class="message"><div class="message-body">' +
        sql +
        "</div></article>"
    );

    webSocket.send(
      JSON.stringify({
        id: sequenceNumber,
        sql: sql,
      })
    );
  }

  $("#terminal-input").keydown(function (event) {
    if (event.keyCode == 13) {
      sendMessage();
    }
  });

  $("#terminal-execute").click(sendMessage);
});
