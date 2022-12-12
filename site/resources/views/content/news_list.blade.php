@extends('layouts.default')
@section('title', $data->ArL_title . ' - ' . trans('common.title'))
@section('site-description', $data->Ar_name)
@section('site-keywords', trans('page-keywords'))
{{-- @section('site-canonical', $canonical) --}}
{{-- @section('page-hreflang', $canonical) --}}
@section('article-headline', $data->ArL_title)
{{-- {{-- @section('article-date', $publishDate) --}}
@section('article-modification-date', $data->created_at)
@section('article-description', trans('page-description'))
@section('article-tag', '')


@section('content')
    @include('layouts.breadcrumb')

    <div class="row">
        <div class="col ">
            <h1 class="t-h1">{{ trans('common.news') }}</h1>
        </div>
    </div>

    <div class="m-article m-container-front">
        {{-- <h1 class="">{{trans('common.news')}}</h1> --}}



        @foreach ($res as $news)
            <div class="row">
                <div class="col-12  m-panel mb-4">
                    <div class="row">
                        <div class="col-xs-12 col-sm-12 col-lg-3 col-xl-3 col-md-3">
                            <a href="{{ lrSeg() }}/{{ App::getLocale() }}/n/{{ $news['MnL_path'] }}">
                                <div class="m-image-f news-list"
                                    style="background: url('/storage{{ $news['media']->ArG_file }}')">
                                </div>
                            </a>
                        </div>
                        <div class="col-sm-12 col-xs-12 col-lg-9 col-xl-9 col-md-9 ">

                            <div class="m-title">
                                <a href="{{ lrSeg() }}/{{ App::getLocale() }}/n/{{ $news['MnL_path'] }}">
                                    {{ $news['MnL_title'] }}
                                    <div class="mt-2 m-date">


                                        {{ df(App::getLocale(), $news['Mn_date'], 0) }}
                                    </div>

                                </a>
                            </div>
                        </div>

                        <div class="m-bd"></div>
                    </div>
                </div>
            </div>
        @endforeach
        <div id="calendar"></div>

        @php
            
            $period = \App\Http\Controllers\CalendarController::archivePeriod(App::getLocale(), 'm_news', Request::get('year'), Request::get('month'), 1, 0);
            
            $prefix = 'n';
            // dd($period);
        @endphp

        @if (count($period) > 0)
            @include('layouts.calendar_list')
        @endif

        @php
            
            $calendar = \App\Http\Controllers\CalendarController::archiveList(App::getLocale(), 'm_news', 1, 0);
            // dd($calendar);
        @endphp
        @include('layouts.calendar')

    </div>

@stop
