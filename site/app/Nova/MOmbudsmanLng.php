<?php

namespace App\Nova;

use Illuminate\Http\Request;
use Laravel\Nova\Fields\ID;
use Laravel\Nova\Fields\Text;
use Waynestate\Nova\CKEditor;
use Laravel\Nova\Fields\Boolean;
use Illuminate\Support\Str;
use Laravel\Nova\Fields\BelongsTo;
use Laravel\Nova\Fields\HasMany;
use Laravel\Nova\Fields\Textarea;
use Laravel\Nova\Http\Requests\NovaRequest;

class MOmbudsmanLng extends Resource
{
    /**
     * The model the resource corresponds to.
     *
     * @var string
     */
    public static $model = \App\Models\MOmbudsmanLng::class;
    public static $displayInNavigation = false;
    // public static $globallySearchable = false;

    /**
     * The single value that should be used to represent the resource when being displayed.
     *
     * @var string
     */
    public static $title = 'ArL_title';

    public static function label()
    {
        return 'Езикова версия Омбудсман';
    }

    /**
     * The columns that should be searched.
     *
     * @var array
     */
    public static $search = [
        'OmbL_title', 'OmbL_intro', 'OmbL_body'
    ];

    /**
     * Get the fields displayed by the resource.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return array
     */
    public function fields(Request $request)
    {
        return [
            ID::make(__('ID'), 'OmbL_id')->sortable(),
            Text::make('Име', 'OmbL_title')->sortable()->rules('required')->displayUsing(function ($text) {
                return Str::limit($text, 200);
            }),


            CKEditor::make('Въведение (челна страница)', 'OmbL_intro')->displayUsing(function ($text) {
                return Str::limit($text, 500);
            }),
            CKEditor::make('Съдържание', 'OmbL_body')->displayUsing(function ($text) {
                return Str::limit($text, 500);
            }),



            BelongsTo::make('Език', 'eq_lng', SLang::class)->rules('required')->withoutTrashed(),

            BelongsTo::make('Омбудсман', 'eq_omb', MOmbudsman::class)->rules('required')->withoutTrashed(),
        ];
    }

    /**
     * Get the cards available for the request.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return array
     */
    public function cards(Request $request)
    {
        return [];
    }

    /**
     * Get the filters available for the resource.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return array
     */
    public function filters(Request $request)
    {
        return [];
    }

    /**
     * Get the lenses available for the resource.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return array
     */
    public function lenses(Request $request)
    {
        return [];
    }

    /**
     * Get the actions available for the resource.
     *
     * @param  \Illuminate\Http\Request  $request
     * @return array
     */
    public function actions(Request $request)
    {
        return [];
    }
}
