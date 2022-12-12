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
            <h1 class="t-h1">{{ $data->ArL_title }}</h1>
        </div>
    </div>
    <div class="m-article m-container-front">
        <div class="m-position">
            <ul class="list">
                @foreach ($res as $news)
                    <li>
                        <div class="content">
                            <a href="{{ lrSeg() }}/{{ App::getLocale() }}/n/{{ $news['MnL_path'] }}">
                                {{ $news['MnL_title'] }}
                                <div class="mt-2 ar-date">
                                    {{ df(App::getLocale(), $news['Mn_date']) }}
                                </div>

                            </a>
                        </div>
                        <div class="m-d"></div>

                    </li>
                @endforeach
            </ul>
        </div>


    </div>





@stop
