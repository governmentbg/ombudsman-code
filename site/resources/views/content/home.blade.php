@extends('layouts.default')
@section('title', trans('common.title'))



@section('content')

    <div class="m-home">

        {{-- news 2 --}}
        <div class="row m-container-front">
            <div class="col-sm-12  col-xs-12 col-lg-7 col-md-8 col-xl-7 m-news">


                @php
                    $data = \App\Http\Controllers\MNewsController::frontNews(App::getLocale(), 1, 0);
                    
                @endphp
                <div class="image-map pb-5" style="background: url('/storage/{{ $data->media->ArG_file }}')">
                    {{-- &nbsp; --}}
                    <div class="m-news-title">
                        <div class="text">
                            <h2 class="title">
                                {{ trans('common.in_focus') }}
                            </h2>
                            <div class="t-d"></div>

                            <a href="{{ lrSeg() }}/{{ App::getLocale() }}/n/{{ $data->MnL_path }}">

                                {{ $data->MnL_title }}
                            </a>
                            {{-- {{$data->Mn_date}} --}}
                            {{-- {{$data->Mn_date->format("m/d/Y")}} --}}
                            <div class="t-date">
                                {{ df(App::getLocale(), $data->Mn_date) }}
                            </div>
                        </div>
                    </div>

                </div>


            </div>
            <div class="col-sm-12  col-xs-12 col-lg-5 col-md-4 col-xl-5 m-media" id="t3">

                @php
                    $cnt = \App\Http\Controllers\MNewsController::countNews(App::getLocale());
                    // dd($cnt);
                    
                    $activeVideo = \App\Http\Controllers\CommonController::getActiveVideo(App::getLocale());
                    
                    if ($activeVideo) {
                        $newsCount = 2;
                    } else {
                        $newsCount = 3;
                    }
                @endphp
                @if ($cnt)

                    @php
                        
                        $newsLine = \App\Http\Controllers\MNewsController::frontNews(App::getLocale(), $newsCount, 1);
                        // dd($newsLine);
                    @endphp

                    <div class="row">
                        <div class="col-xl-12 col-sm-12 col-xs-12 col-lg-12 col-md-12 mb-0">


                        </div>
                    </div>

                    @foreach ($newsLine as $news)
                        {{-- {{dd($news["media"]->ArG_file)}} --}}
                        {{-- {{dd($news["media"])}} --}}
                        {{-- {{$news->MnL_title}} --}}
                        {{-- {{ $news }} --}}

                        <div class="row">
                            <div class="col-xl-12 col-sm-12 col-xs-12 col-lg-12 col-md-12 m-panel">

                                <div class="row">
                                    <div class="col-xl-4 col-sm-12 col-xs-12 col-lg-4 col-md-5">
                                        <a href="{{ lrSeg() }}/{{ App::getLocale() }}/n/{{ $news['MnL_path'] }}">
                                            <div class="m-image-f"
                                                style="background: url('/storage{{ $news['media']->ArG_file }}')">

                                            </div>
                                        </a>
                                    </div>
                                    <div class="col-xl-8 col-sm-12 col-xs-12 col-lg-8 col-md-7">
                                        <div class="m-title">
                                            <a
                                                href="{{ lrSeg() }}/{{ App::getLocale() }}/n/{{ $news['MnL_path'] }}">
                                                {{ $news['MnL_title'] }}
                                                <div class="m-date">


                                                    {{ df(App::getLocale(), $news['Mn_date']) }}
                                                </div>
                                            </a>
                                        </div>
                                    </div>
                                </div>
                                <div class="m-bd"></div>

                            </div>
                        </div>
                    @endforeach
                @endif


                @if ($activeVideo)
                    @php
                        $homeVideo = \App\Http\Controllers\CommonController::showActiveVideo(App::getLocale());
                        // dd($newsLine);
                    @endphp
                    <div class="row">
                        <div class="col-xl-12 col-sm-12 col-xs-12 col-lg-12 col-md-12 m-panel ">

                            <div class="row">
                                <div class="col-xl-4 col-sm-12 col-xs-12 col-lg-4 col-md-5">
                                    <div class="m-image-f">
                                        @if ($homeVideo->Str_embed)
                                            <x-embed url="{{ $homeVideo->Str_embed }}" class="mb-3" />
                                        @endif
                                    </div>

                                </div>
                                <div class="col-xl-8 col-sm-12 col-xs-12 col-lg-8 col-md-7">
                                    <div class="m-title ps-rel">
                                        <a href="{{ lrSeg() }}/{{ App::getLocale() }}/p/sabitiya">

                                            {!! $homeVideo->MvL_body !!}

                                        </a>
                                    </div>

                                </div>
                            </div>




                        </div>
                    </div>
                @endif


            </div>
            <script>
                new PerfectScrollbar('#t3');
            </script>
        </div>


        {{-- news 2 end --}}

        {{-- incoming start --}}
        <div class="row m-events">
            <div class="col ">

                <div class="m-bd">

                </div>
                <h2 class="m-ev-title">
                    {{ trans('common.events') }}
                </х>

            </div>

        </div>

        <div class="row mt-5 mb-5">

            @php
                $data = \App\Models\MEvent::getEvents(i18n(App::getLocale()), 4);
                
            @endphp



            @foreach ($data as $ev)
                {{-- {{ dd($ev) }} --}}
                <div class="col-xl-3 col-sm-12 col-xs-12 col-lg-3 col-md-3 m-events-list mb-4">
                    {{-- --- {{ $ev->MvL_id }} ---- --}}
                    <div class="title">
                        <a
                            href="{{ lrSeg() }}/{{ App::getLocale() }}/p/{{ getPathById(App::getLocale(), 41) }}#e{{ $ev->MvL_id }}">
                            {{ $ev->MvL_title }}
                        </a>
                    </div>
                    <div class="date">
                        {{ df(App::getLocale(), $ev->Mv_date, 1) }}
                    </div>
                </div>
            @endforeach





        </div>
        {{-- incoming end --}}
        {{-- bio start --}}
        {{-- bio end --}}




    </div>




@stop

@section('content_bio')
    <div class="m-accent m-home">
        <div class="container">

            <div class="row omb-front">
                <div class="col-sm-12  col-xs-12 col-lg-6 col-md-5 col-xl-7 ">
                    <div class="content-box">
                        <div class="omb-bio">
                            @php
                                $omb = \App\Http\Controllers\MOmbudsmanController::getCurrentOmb(App::getLocale());
                                
                            @endphp
                            {{-- <div class="image-map" style="background: url('/storage/{{ $omb->Omb_photo }}')">


                            </div> --}}
                            <h2 class="common-title mt-4">

                                {{ trans('common.title') }}
                            </h2>

                            <div class="bio" id="bio">
                                <div class="bio2" id="bio2">

                                    {!! $omb->OmbL_intro !!}

                                </div>



                                <div class="link-m  mt-2 mb-4">
                                    <a href="{{ lrSeg() }}/{{ App::getLocale() }}/p/{{ $omb->ArL_path }}">
                                        {{ trans('common.read_more') }}
                                        »
                                    </a>
                                </div>


                            </div>


                        </div>
                    </div>

                    <script>
                        new PerfectScrollbar('#bio2');
                    </script>
                </div>


            </div>

            {{-- accent end --}}
        </div>
    </div>
@stop

@section('content1')
    <div class="m-accent-2 m-home">
        <div class="container">
            {{-- accent start --}}
            <div class="row acc1">
                <div class="col-sm-12  col-xs-12 col-lg-3 col-md-3 col-xl-3 m-acc m-border">
                    <a href="{{ lrSeg() }}/{{ App::getLocale() }}/p/{{ getPathById(App::getLocale(), 20) }}">
                        <div class="content-box">

                            <div>
                                <img src="/img/bg_box1.png" class="img-box" alt="{{ trans('common.box_rights') }}" />
                            </div>

                            <h3 class="acc-title">
                                {{ trans('common.box_rights') }}
                            </h3>
                        </div>
                    </a>
                </div>
                <div class="col-sm-12  col-xs-12 col-lg-3 col-md-3 col-xl-3 m-acc m-border">
                    <a href="{{ lrSeg() }}/{{ App::getLocale() }}/p/{{ getPathById(App::getLocale(), 55) }}">
                        <div class="content-box">
                            <div>
                                <img src="/img/bg_box2.png" alt=" {{ trans('common.box_omb2') }}" class="img-box" />
                            </div>
                            <h3 class="acc-title">
                                {{ trans('common.box_omb2') }}
                            </h3>

                        </div>
                    </a>
                </div>
                <div class="col-sm-12  col-xs-12 col-lg-3 col-md-3 col-xl-3 m-acc m-border">
                    <a
                        href="{{ lrSeg() }}/prava-na-deteto/{{ App::getLocale() }}/p/{{ getPathById(App::getLocale(), 37) }}">
                        <div class="content-box">
                            <div>
                                <img src="/img/bg_box3.png" alt=" {{ trans('common.box_child') }}" class="img-box" />
                            </div>
                            <h3 class="acc-title">
                                {{ trans('common.box_child') }}
                            </h3>

                        </div>
                    </a>
                </div>
                <div class="col-sm-12  col-xs-12 col-lg-3 col-md-3 col-xl-3 m-acc">
                    <a href="{{ lrSeg() }}/{{ App::getLocale() }}/p/{{ getPathById(App::getLocale(), 43) }}">
                        <div class="content-box">
                            <div>
                                <img src="/img/bg_box4.png" alt="{{ trans('common.box_pub') }}" class="img-box" />
                            </div>
                            <h3 class="acc-title">
                                {{ trans('common.box_pub') }}
                            </h3>

                        </div>
                    </a>
                </div>
            </div>

            {{-- accent end --}}
        </div>
    </div>
@stop


@section('content2')

    <div class="container m-home m-home-leg m-home-leg">
        <div class="row">

            <div class="col-sm-12  col-xs-12 col-xl-4 col-lg-4  col-md-4">

                <h2 class="m-home-title mt-4 mr-5">{{ trans('common.positions_t') }}</h2>


                {{-- </div> --}}
                {{-- <h2 class="m-home-title mt-4 mr-5">{{ trans('common.box_position') }}</h2> --}}

                <div class="m-position ps-rel1" id="s1">
                    <ul class="list">

                        @if (App::getLocale() == 'bg')

                            @foreach (\App\Http\Controllers\MPositionController::pstList(5, 1) as $ps)
                                <li>
                                    <div class="content">

                                        <a href="{{ lrSeg() }}/{{ App::getLocale() }}/d/{{ $ps->Pst_path }}">
                                            {{ $ps->Pst_name }}
                                        </a>
                                        {{-- <a href="{{ lrSeg() }}/storage{{ $ps->Pst_file }}" download="">
                                            {{ $ps->Pst_name }}
                                        </a> --}}
                                    </div>

                                    <div class="m-d"></div>

                                </li>
                            @endforeach

                        @endif
                    </ul>
                </div>
                {{-- <script>
                    new PerfectScrollbar('#s1');
                </script> --}}
                <div class="link-m"> <a
                        href="{{ lrSeg() }}/{{ App::getLocale() }}/p/{{ getPathById(App::getLocale(), 15) }}">
                        {{ trans('common.all_positions') }} » </a>
                </div>

            </div>
            <div class="col-sm-12  col-xs-12 col-xl-4 col-lg-4  col-md-4 m-home-leg">

                <h2 class="m-home-title mt-4 mr-5">{{ trans('common.requests_sc') }}</h2>


                <div class="m-position ps-rel1" id="s2">
                    <ul class="list">
                        @if (App::getLocale() == 'bg')
                            @foreach (\App\Http\Controllers\MPositionController::pstList(5, 2) as $ps)
                                <li>
                                    <div class="content">
                                        <a href="{{ lrSeg() }}/{{ App::getLocale() }}/d/{{ $ps->Pst_path }}">
                                            {{ $ps->Pst_name }}
                                        </a>

                                        {{-- <a href="{{ lrSeg() }}/storage{{ $ps->Pst_file }}" download="">
                                            {{ $ps->Pst_name }}
                                        </a> --}}
                                    </div>

                                    <div class="m-d"></div>

                                </li>
                            @endforeach

                        @endif
                    </ul>
                </div>
                {{-- <script>
                    new PerfectScrollbar('#s2');
                </script> --}}
                <div class="link-m"> <a
                        href="/{{ App::getLocale() }}/p/{{ getPathById(App::getLocale(), 51) }}">{{ trans('common.all_requests_sc') }}
                        »
                    </a>
                </div>

            </div>
            <div class="col-sm-12  col-xs-12 col-xl-4 col-lg-4  col-md-4 m-home-leg">

                {{-- @include('content.actual_box') --}}

                <h2 class="m-home-title mt-4 mr-5">{{ trans('common.trending') }}</h2>


                <div class="m-position ps-rel1" id="s3">
                    <ul class="list">
                        @php
                            
                            $data = \App\Http\Controllers\MNewsController::frontNews(App::getLocale(), 10, 0, 2, 0);
                        @endphp
                        {{-- {{ dd($data) }} --}}
                        @foreach ($data as $act)
                            {{-- {{ dd($act['MnL_title']) }} --}}
                            <li>
                                <div class="content">
                                    <a href="{{ lrSeg() }}/{{ App::getLocale() }}/n/{{ $act['MnL_path'] }}">
                                        {{ $act['MnL_title'] }}
                                    </a>
                                </div>
                                <div class="m-d"></div>

                            </li>
                        @endforeach
                    </ul>
                </div>
                {{-- <script>
                    new PerfectScrollbar('#s3');
                </script> --}}
                <div class="link-m">
                    <a href="{{ lrSeg() }}/{{ App::getLocale() }}/p/{{ getPathById(App::getLocale(), 52) }}">{{ trans('common.all_pub') }}
                        » </a>
                </div>

            </div>
        </div>
    </div>

@stop
