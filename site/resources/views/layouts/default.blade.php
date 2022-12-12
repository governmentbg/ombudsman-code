<!DOCTYPE html>
<html lang="{{ str_replace('_', '-', app()->getLocale()) }}">

<head>
    @include('layouts.head')

    {{-- <script src="/js/jquery-3.3.1.min.js"></script> --}}
    <script src="/js/jquery-3.6.0.min.js"></script>
    <script src="/js/jquery-3.6.0.min.map"></script>
    <script src="/js/perfect-scrollbar.min.js"></script>
    {{-- <script src="/js/perfect-scrollbar.min.js.map"></script> --}}
</head>

<body class="antialiased @if (Request::segment(1) == 'lr') lr @endif @if (Request::segment(1) == 'prava-na-deteto') cr @endif">
    @if (Request::segment(1) == 'lr1')
    @include('layouts.header_lr')
    @else
    @include('layouts.header')
    @endif

    <div class="clearfix">

    </div>
    @include('layouts.header_claim')
    <div class="container mt-2 res">

        @if (Request::segment(1) == 'lr1')
        <div class="row">
            <div class="col-sm-12 col-xs-12 col-lg-3 col-md-3 col-xl-3">
                @include('layouts.nav_lr')

            </div>
            <div class="col-sm-12 col-xs-12 col-lg-9 col-md-9 col-xl-9"> @yield('content')</div>
        </div>
        @else
        @yield('header_claim_front')
        @yield('content')
        @endif




    </div>

    @yield('content_bio')
    @yield('content1')

    @yield('content2')



    {{-- </div> --}}
    {{-- @if (Request::segment(1) != 'prava-na-deteto') --}}
    <div class="footer">
        <div class="container">
            <div class="footer-body">
                <div class="row mt-3 mb-3">
                    <div class="col-sm-12 col-xs-12 col-md-3 col-lg-3  col-xl-3 text-left mt-3">
                        <h6>{{ trans('common.reception') }}</h6>
                        <ul class="list-unstyled">
                            <li>{!! trans('common.address') !!}</li>
                            <li>{{ trans('common.phone') }}</li>
                            <li>e-mail: <a href="mailto:priemna@ombudsman.bg">priemna@ombudsman.bg</a></li>

                        </ul>
                    </div>
                    <div class="col-sm-12 col-xs-12 col-md-3 col-lg-3  col-xl-3 text-left mt-3">

                        <h6>{{ trans('common.presscenter') }}</h6>
                        <ul class="list-unstyled">
                            <li>{{ trans('common.phone2') }}</li>
                            <li>e-mail: <a href="press:priemna@ombudsman.bg">press@ombudsman.bg</a></li>

                        </ul>

                    </div>
                    <div class="col-sm-12 col-xs-12 col-md-2 col-lg-2  col-xl-2 text-left mt-3 mb-3">


                        <h6>{{ trans('common.followUs') }}</h6>
                        <div class="media-links">
                            @php
                            $data = getConfigByKey(2, null, null, 10);

                            @endphp
                            @if ($data)
                            <span class="badge">

                                <a href=""> <i class="cib-twitter"></i> </a>
                            </span>
                            @endif
                            @php
                            $data = getConfigByKey(2, null, null, 9);

                            @endphp
                            @if ($data)
                            <span class="badge">

                                <a href="{{ $data->Cf_value }}" target="_blank"> <i class="cib-facebook-f"></i>
                                </a>
                            </span>
                            @endif
                            @php
                            $data = getConfigByKey(2, null, null, 8);

                            @endphp
                            @if ($data)
                            <span class="badge">

                                <a href="{{ $data->Cf_value }}" target="_blank"><i class="cib-youtube"></i> </a>
                            </span>
                            @endif

                            <div class="row mt-4">
                                <div class="col-xl-12 col-lg-12 col-md-12 col-sm-6 col-xs-6">
                                    @php
                                    $data = getConfigByKey(2, null, null, 7);
                                    // dd($data);
                                    @endphp
                                    @if ($data)
                                    <a href="{{ $data->Cf_value }}" target="_blank">
                                        <img src="/img/apple_store.svg" width="100px" alt="" class="mr-3 mt-2" /></a>
                                    @endif
                                </div>
                                <div class="col-xl-12 col-lg-12 col-md-12 col-sm-6 col-xs-6">
                                    @php
                                    $data = getConfigByKey(2, null, null, 6);
                                    // dd($data);
                                    @endphp
                                    @if ($data)
                                    <a href="{{ $data->Cf_value }}" target="_blank">
                                        <img src="/img/google-play.svg" width="100px" alt="" class="mr-3 mt-2" /></a>
                                    @endif
                                </div>

                            </div>






                        </div>


                    </div>

                    <div class="col-sm-12 col-xs-12 col-md-4 col-lg-4  col-xl-4 text-left mt-3">
                        <a href="{{ lrSeg() }}/{{ App::getLocale() }}/p/{{ getPathById(App::getLocale(), 557) }}">
                            <div class="project-data">

                                <div class="icons text-center">
                                    <img src="/img/icon_eu.png" alt="" class="mr-3 mb-2" />
                                    <img src="/img/icon_obdu.png" alt="" />

                                </div>

                                <div class="text">
                                    {{ trans('common.project') }}
                                </div>
                            </div>
                        </a>



                    </div>
                </div>

            </div>
            <div class="row">
                <div class="col-12 mb-2">
                    <div class="cp">
                        © {{ date('Y') }} {{ trans('common.title') }}</div>
                </div>
            </div>


        </div>

    </div>
    {{-- @endif --}}


    {{-- <script src="/js/jquery-3.3.1.min.js"></script> --}}
    <script src="/js/popper.min.js"></script>
    <script src="/js/bootstrap.min.js"></script>
    <script src="/js/jquery.sticky.js"></script>
    <script src="/js/main.js"></script>

    <script src="/js/ekko-lightbox.min.js"></script>
    <script src="/js/ekko-lightbox.min.js.map"></script>
    <script src="/js/select2.min.js"></script>

    <script src="/js/datepicker.min.js"></script>
    <script src="/js/bg-bg.min.js"></script>
    <script src="/js/video.min.js"></script>
    {{-- <script src="/js/perfect-scrollbar.min.js"></script>
    <script src="/js/perfect-scrollbar.min.js.map"></script> --}}


    <script>
        $(document).on('click', '[data-toggle="lightbox"]', function(event) {
            event.preventDefault();
            $(this).ekkoLightbox();
        });


        $(document).ready(function() {
            $('.js-example-basic-single').select2();
        });

        $("#datepicker").datepicker({
            uiLibrary: "bootstrap4",
            locale: "bg-bg",
            format: "dd/mm/yyyy",
            weekStartDay: 1,

        });
        $("#datepicker1").datepicker({
            uiLibrary: "bootstrap4",
            locale: "bg-bg",
            format: "dd/mm/yyyy",
            weekStartDay: 1,

        });




        $(document).ready(function() {
            var resize = new Array('.res', '.m-home-title', '.m-title', 'h2', '.card-body', '.content', '.list');
            resize = resize.join(',');

            //resets the font size when "reset" is clicked
            var resetFont = $(resize).css('font-size');
            $(".reset").click(function() {
                $(resize).css('font-size', resetFont);
            });

            //increases font size when "+" is clicked
            $(".increase").click(function() {
                var originalFontSize = $(resize).css('font-size');
                var originalFontNumber = parseFloat(originalFontSize, 10);
                var newFontSize = originalFontNumber * 1.2;
                $(resize).css('font-size', newFontSize);
                return false;
            });

            //decrease font size when "-" is clicked

            $(".decrease").click(function() {
                var originalFontSize = $(resize).css('font-size');
                var originalFontNumber = parseFloat(originalFontSize, 10);
                var newFontSize = originalFontNumber * 0.8;
                $(resize).css('font-size', newFontSize);
                return false;
            });

        });



        // $('#sandbox-container .input-group.date').datepicker({
        // });
    </script>

</body>

</html>