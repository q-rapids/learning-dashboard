var isSi = false;
var isdsi = false;
var isqf = false;
var isdqf = true;

var urlpred; // to get prediction data
var urlhist; // to get historical data
if (getParameterByName('id').length !== 0) {
    urlpred = parseURLComposed("../api/qualityFactors/metrics/prediction");
    urlhist = parseURLComposed("../api/qualityFactors/metrics/historical");
} else {
    var profileId = sessionStorage.getItem("profile_id");
    urlpred = parseURLComposed("../api/qualityFactors/metrics/prediction?profile="+profileId);
    urlhist = parseURLComposed("../api/qualityFactors/metrics/historical?profile="+profileId);
}

//initialize data vectors
var texts = [];
var ids = [];
var labels = [];
var value = [];
var errors = [];

var categories = [];

function getData() {
    document.getElementById("loader").style.display = "block";
    document.getElementById("chartContainer").style.display = "none";
    texts = [];
    ids = [];
    labels = [];
    value = [];
    errors = [];
    var technique = $("#selectedTechnique").text();
    var dateFrom = new Date($('#datepickerFrom').val());
    var dateC = new Date($('#datepickerCurrentDate').val());
    var dateTo = new Date($('#datepickerTo').val());
    var timeDiff = dateTo.getTime() - dateC.getTime();
    var diffDays = Math.ceil(timeDiff / (1000 * 3600 * 24));
    if (diffDays < 1) {
        warningUtils("Warning", "To date has to be bigger than from date");
        //alert('To date has to be bigger than from date');
    } else {
        //get predicted data from API
        jQuery.ajax({
            dataType: "json",
            url: urlpred,
            data: {
                "technique": technique,
                "horizon": diffDays
            },
            cache: false,
            type: "GET",
            async: true,
            success: function (response) {
                var data = response;
                sortDataAlphabetically(data);
                //get historical data from API
                jQuery.ajax({
                    dataType: "json",
                    url: urlhist,
                    data: {
                        "from": parseDate(dateFrom),
                        "to": parseDate(dateC)
                    },
                    cache: false,
                    type: "GET",
                    async: true,
                    success: function (response) {
                        var data_hist = response;
                        sortDataAlphabetically(data_hist);
                        // generate historical serie of values
                        for (i = 0; i < data_hist.length; ++i) {
                            //for each qf save name to texts vector and id to ids vector
                            if (data_hist[i].metrics.length > 0) {
                                texts.push(data_hist[i].name);
                                ids.push(data_hist[i].id);
                                value.push([[]]);
                                last = data_hist[i].metrics[0].id;
                                labels.push([data_hist[i].metrics[0].name]);
                                k = 0;
                                for (j = 0; j < data_hist[i].metrics.length; ++j) {
                                    //check if we are still on the same metric
                                    if (last !== data_hist[i].metrics[j].id) {
                                        // New metric
                                        labels[i].push(data_hist[i].metrics[j].name);
                                        last = data_hist[i].metrics[j].id;
                                        ++k;
                                        value[i].push([]);
                                    }
                                    //push date and value to values vector
                                    if (!isNaN(data_hist[i].metrics[j].value)) {
                                        value[i][k].push(
                                            {
                                                x: data_hist[i].metrics[j].date,
                                                y: data_hist[i].metrics[j].value
                                            }
                                        );
                                    }
                                }
                            } else {
                                data_hist.splice(i, 1);
                                --i;
                            }
                        }
                        // add prediction series generated
                        for (i = 0; i < data.length; ++i) {
                            // order data
                            data[i].metrics.sort((a,b) => (a.name > b.name) ? 1 : ((b.name > a.name) ? -1 : 0));
                            //for each qf save name to texts vector and id to ids vector
                            if (data[i].metrics.length > 0) {
                                value[i].push([]);
                                last = data[i].metrics[0].id;
                                labels[i].push("Predicted "+data[i].metrics[0].name);
                                errors.push([data[i].metrics[0].forecastingError]);
                                k = value[i].length-1;
                                for (j = 0; j < data[i].metrics.length; ++j) {
                                    //check if we are still on the same metric
                                    if (last != data[i].metrics[j].id) {
                                        labels[i].push("Predicted "+data[i].metrics[j].name);
                                        last = data[i].metrics[j].id;
                                        k++;
                                        value[i].push([]);
                                        errors[i].push(data[i].metrics[j].forecastingError);
                                    }
                                    //push date and value to values vector
                                    if (!isNaN(data[i].metrics[j].value)) {
                                        if (data[i].metrics[j].value !== null) {
                                            value[i][k].push(
                                                {
                                                    x: data[i].metrics[j].date,
                                                    y: data[i].metrics[j].value
                                                }
                                            );
                                        }
                                    }
                                }
                            } else {
                                data.splice(i, 1);
                                --i;
                            }
                        }
                        document.getElementById("loader").style.display = "none";
                        document.getElementById("chartContainer").style.display = "block";
                        getMetricsCategories();
                    }});
            },
            error: function (xhr, ajaxOptions, thrownError) {
                document.getElementById("loader").style.display = "none";
                document.getElementById("chartContainer").style.display = "block";
                document.getElementById("chartContainer").innerHTML = "Error " + xhr.status;
            }
        });
    }
    console.log(errors);
    console.log(value);
    console.log(labels);
    console.log(texts);
    console.log(ids);
}

function sortDataAlphabetically (data) {
    function compare (a, b) {
        if (a.name < b.name) return -1;
        else if (a.name > b.name) return 1;
        else return 0;
    }
    data.sort(compare);
}

function getMetricsCategories () {
    jQuery.ajax({
        url: "../api/metrics/categories",
        type: "GET",
        async: true,
        success: function (response) {
            categories = response;
            drawChart();
        }
    });
}

window.onload = function() {
    getData();
};