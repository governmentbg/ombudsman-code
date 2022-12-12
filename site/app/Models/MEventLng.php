<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\SoftDeletes;
use Illuminate\Support\Str;

/**
 * @property int    $MvL_id
 * @property int    $Mv_id
 * @property int    $S_Lng_id
 * @property int    $created_at
 * @property int    $updated_at
 * @property int    $deleted_at
 * @property string $MvL_path
 * @property string $MvL_title
 * @property string $MvL_body
 * @property string $MvL_meta
 */
class MEventLng extends Model
{
    use SoftDeletes;
    /**
     * The database table used by the model.
     *
     * @var string
     */
    protected $table = 'm_event_lng';

    /**
     * The primary key for the model.
     *
     * @var string
     */
    protected $primaryKey = 'MvL_id';

    /**
     * Attributes that should be mass-assignable.
     *
     * @var array
     */
    protected $fillable = [
        'Mv_id', 'S_Lng_id', 'MvL_path', 'MvL_title', 'MvL_body', 'MvL_meta', 'created_at', 'updated_at', 'deleted_at'
    ];

    /**
     * The attributes excluded from the model's JSON form.
     *
     * @var array
     */
    protected $hidden = [];

    /**
     * The attributes that should be casted to native types.
     *
     * @var array
     */
    protected $casts = [
        'MvL_id' => 'int', 'Mv_id' => 'int', 'S_Lng_id' => 'int', 'MvL_path' => 'string', 'MvL_title' => 'string', 'MvL_body' => 'string', 'MvL_meta' => 'string',  'St_id' => 'int', 'created_at' => 'timestamp', 'updated_at' => 'timestamp', 'deleted_at' => 'timestamp'
    ];

    /**
     * The attributes that should be mutated to dates.
     *
     * @var array
     */
    protected $dates = [
        'created_at', 'updated_at', 'deleted_at'
    ];

    /**
     * Indicates if the model should be timestamped.
     *
     * @var boolean
     */
    public $timestamps = false;

    public static function boot()
    {
        parent::boot();


        static::creating(function ($article) {
            if ($article->S_Lng_id == 2) {
                $article->MvL_path =   Str::slug(C2L($article->MvL_title));
            }
            $article->created_at = now();
            $article->updated_at = now();
        });

        static::saving(function ($article) {

            $truncated = substr($article->MnL_title, 0, 80);
            $article->MvL_path = Str::slug(C2L($truncated));
            $article->updated_at = now();
        });
    }

    // Scopes...

    // Functions ...

    // Relations ...

    public function eq_lng()
    {
        return $this->belongsTo(SLang::class, 'S_Lng_id');
    }
    public function eq_event()
    {
        return $this->belongsTo(MEvent::class, 'Mv_id');
    }

    public function eq_files_event()
    {
        return $this->hasMany(MFiles::class, 'MvL_id');
    }
}
